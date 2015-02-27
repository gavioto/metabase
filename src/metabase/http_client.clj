(ns metabase.http-client
  "HTTP client for making API calls against the Metabase API. For test/REPL purposes."
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [metabase.util :as u]))

(declare authenticate
         build-url
         -client)

;; ## API CLIENT

(def ^:dynamic *url-prefix*
  "Prefix to automatically prepend to the URL of calls made with `client`."
  "http://localhost:3000/api/")

(defn
  ^{:arglists ([credentials-map? method expected-status-code? url http-body-map? & url-kwargs])}
  client
  "Perform an API call and return the response (for test purposes).
   The first arg after URL will be passed as a JSON-encoded body if it is a map.
   Other &rest kwargs will be passed as `GET` parameters.

   Args:
   *  CREDENTIALS-MAP       Optional map of `:email` and `:password` of a User whose credentials we should perform the request with.
   *  METHOD                `:get`, `:post`, `:delete`, or `:put`
   *  EXPECTED-STATUS-CODE  When passed, throw an exception if the response has a different status code.
   *  URL                   Base URL of the request, which will be appended to `*url-prefix*`. e.g. `card/1/favorite`
   *  HTTP-BODY-MAP         Optional map to send a the JSON-serialized HTTP body of the request
   *  URL-KWARGS            key-value pairs that will be encoded and added to the URL as GET params

  examples:

    (client :get 200 \"card/1\")                ; GET  http://localhost:3000/api/card/1, throw exception is status code != 200
    (client :get \"card\" :org 1)               ; GET  http://localhost:3000/api/card?org=1
    (client :post \"card\" {:name \"My Card\"}) ; POST http://localhost:3000/api/card with JSON-encoded body {:name \"My Card\"}"
  [& args]
  (let [[credentials [method & args]] (u/optional map? args)
        [expected-status [url & args]] (u/optional integer? args)
        [body [& {:as url-param-kwargs}]] (u/optional map? args)]
    (-client credentials method expected-status url body url-param-kwargs)))


;; ## INTERNAL FUNCTIONS

(defn- -client [credentials method expected-status url http-body url-param-kwargs]
  ;; Since the params for this function can get a little complicated make sure we validate them
  {:pre [(or (nil? credentials)
             (map? credentials))
         (contains? #{:get :post :put :delete} method)
         (or (nil? expected-status)
             (integer? expected-status))
         (string? url)
         (or (nil? http-body)
             (map? http-body))
         (or (nil? url-param-kwargs)
             (map? url-param-kwargs))]}

  (let [request-map {:content-type :json
                     :accept :json
                     :headers {"X-METABASE-SESSION" (when credentials (authenticate credentials))}
                     :body (json/write-str http-body)}
        request-fn (case method
                     :get  client/get
                     :post client/post
                     :put client/put
                     :delete client/delete)
        url (build-url url url-param-kwargs)
        method-name (.toUpperCase ^String (name method))

        ;; Now perform the HTTP request
        {:keys [status body]} (try (request-fn url request-map)
                                   (catch clojure.lang.ExceptionInfo e
                                     (println method-name url)
                                     (-> (.getData ^clojure.lang.ExceptionInfo e)
                                         :object)))]

    ;; -check the status code if EXPECTED-STATUS was passed
    (println method-name url status) status
    (when expected-status
      (when-not (= status expected-status)
        (println body)
        (throw (Exception. (format "%s %s expected a status code of %d, got %d" method-name url expected-status status)))))

    ;; Deserialize the JSON response or return as-is if that fails
    (try (-> body
             json/read-str
             clojure.walk/keywordize-keys)
         (catch Exception _
           body))))

(defn- authenticate [{:keys [email password] :as credentials}]
  {:pre [(string? email)
         (string? password)]}
  (try
    (-> (client :post 200 "session" credentials)
        :id)
    (catch Exception e
      (println "Failed to authenticate with email:" email "and password:" password ". Does user exist?"))))

(defn- build-url [url url-param-kwargs]
  {:pre [(string? url)
         (or (nil? url-param-kwargs)
             (map? url-param-kwargs))]}
  (str *url-prefix* url (when-not (empty? url-param-kwargs)
                          (str "?" (->> url-param-kwargs
                                        (map (fn [[k v]]
                                               [(if (keyword? k) (name k) k)
                                                (if (keyword? v) (name v) v)]))
                                        (map (partial interpose "="))
                                        (map (partial apply str))
                                        (interpose "&")
                                        (apply str))))))