import React, { Component, PropTypes } from "react";
import { Link } from "react-router";
import pure from "recompose/pure";
import cx from "classnames";
import Icon from "metabase/components/Icon"

import {
    getQuestionUrl,
    typeToBgClass,
    typeToLinkClass,
} from "../utils";

const GuideDetail = ({
    entity = {},
    tables,
    type,
    exploreLinks,
    detailLabelClasses
}) => {
    const title = entity.display_name || entity.name;
    const { caveats, points_of_interest } = entity;
    const typeToLink = {
        dashboard: `/dash/${entity.id}`,
        metric: getQuestionUrl({
            dbId: tables[entity.table_id] && tables[entity.table_id].db_id,
            tableId: entity.table_id,
            metricId: entity.id
        }),
        segment: getQuestionUrl({
            dbId: tables[entity.table_id] && tables[entity.table_id].db_id,
            tableId: entity.table_id,
            segmentId: entity.id
        }),
        table: getQuestionUrl({
            dbId: entity.db_id,
            tableId: entity.id
        })
    };
    const link = typeToLink[type];
    const typeToLearnMoreLink = {
        metric: `/reference/metrics/${entity.id}`,
        segment: `/reference/segments/${entity.id}`,
        table: `/reference/databases/${entity.db_id}/tables/${entity.id}`
    };
    const learnMoreLink = typeToLearnMoreLink[type];

    const linkClass = typeToLinkClass[type]
    const linkHoverClass = `${typeToLinkClass[type]}-hover`
    const bgClass = typeToBgClass[type]
    const hasLearnMore = type === 'metric' || type === 'segment' || type === 'table';
    const interestingOrImportant = type === 'dashboard' ? 'important' : 'interesting';

    return <div className="relative mt2 pb3">
        <div className="flex align-center">
            <div style={{
                width: 40,
                height: 40,
                left: -60
            }}
            className={cx('absolute text-white flex align-center justify-center', bgClass)}
            >
                <Icon name={type === 'metric' ? 'ruler' : type} />
            </div>
            { title && <ItemTitle link={link} title={title} linkColorClass={linkClass} linkHoverClass={linkHoverClass} /> }
        </div>
        <div className="mt2">
            <ContextHeading>
                { `Why this ${type} is ${interestingOrImportant}` }
            </ContextHeading>

            <ContextContent empty={!points_of_interest}>
                {points_of_interest || `Nothing ${interestingOrImportant} yet`}
            </ContextContent>

            <div className="mt2">
                <ContextHeading>
                    {`Things to be aware of about this ${type}`}
                </ContextHeading>

                <ContextContent empty={!caveats}>
                    {caveats || 'Nothing to be aware of yet'}
                </ContextContent>
            </div>

            { exploreLinks && exploreLinks.length > 0 && [
                <div className="mt2">
                    <ContextHeading key="detailLabel">Explore this metric</ContextHeading>,
                    <div key="detailLinks">
                        <Link className="text-brand inline-block mr2 link text-bold" to={link}>View this metric</Link>
                        { exploreLinks.map(link =>
                            <Link
                                className="inline-block text-bold text-brand mr2 link"
                                key={link.url}
                                to={link.url}
                            >
                                {`By ${link.name}`}
                            </Link>
                        )}
                    </div>
                </div>
            ]}
            { hasLearnMore &&
                <Link
                    className={cx('block mt3 no-decoration text-underline-hover text-bold', linkClass)}
                    to={learnMoreLink}
                >
                    Learn more
                </Link>
            }
        </div>
    </div>;
};

GuideDetail.propTypes = {
    entity: PropTypes.object,
    type: PropTypes.string,
    exploreLinks: PropTypes.array
};

const ItemTitle = ({ title, link, linkColorClass, linkHoverClass }) =>
    <h2>
        <Link
            className={ cx(linkColorClass, linkHoverClass) }
            style={{ textDecoration: 'none' }}
            to={ link }
        >
            { title }
        </Link>
    </h2>

const ContextHeading = ({ children }) =>
    <h3 className="mb1 text-grey-4">{ children }</h3>

const ContextContent = ({ empty, children }) =>
    <p className={cx('m0 text-paragraph text-measure', { 'text-grey-3': empty })}>
        { children }
    </p>


export default pure(GuideDetail);
