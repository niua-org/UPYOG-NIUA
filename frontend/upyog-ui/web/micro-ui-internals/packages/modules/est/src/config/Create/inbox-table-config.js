import React from "react";
import { Link } from "react-router-dom";

// applies a style to the inputs of cells
const GetCell = (value) => <span className="cell-text">{value}</span>;
const GetMobCell = (value) => <span className="sla-cell">{value}</span>;

/**
 * TableConfig component renders the table columns in inbox of Estate Management employee side
 * @returns Returns columns for the inbox table
 */
export const TableConfig = (t) => ({
  EST: {
    inboxColumns: (props) => [
      {
        Header: t("EST_ALLOTMENT_ID"),
        Cell: ({ row }) => {
          return (
            <div>
              <span className="link">
                <Link to={`${props.parentRoute}/application-details/` + `${row?.original?.allotmentId}`}>
                  {row.original?.allotmentId}
                </Link>
              </span>
            </div>
          );
        },
        mobileCell: (original) => GetMobCell(original?.allotmentId),
      },

      {
        Header: t("EST_ASSET_NUMBER"),
        Cell: (row) => {
          return GetCell(`${row?.cell?.row?.original?.assetNo}`)
        },
        mobileCell: (original) => GetMobCell(original?.assetNo),
      }, 

      {
        Header: t("EST_ALLOTTEE_NAME"),
        Cell: (row) => {
          return GetCell(`${row?.cell?.row?.original?.alloteeName}`)
        },
        mobileCell: (original) => GetMobCell(original?.alloteeName),
      },
      
      {
        Header: t("EST_MOBILE_NUMBER"),
        Cell: (row) => {
          return GetCell(`${row?.cell?.row?.original?.mobileNo}`)
        },
        mobileCell: (original) => GetMobCell(original?.mobileNo),
      },
      
      {
        Header: t("EST_STATUS"),
        Cell: ({ row }) => {
          return GetCell(`${row?.original?.status}`);
        },
        mobileCell: (original) => GetMobCell(original?.status),
      },
      
    ],
    serviceRequestIdKey: (original) => original?.[t("EST_INBOX_UNIQUE_APPLICATION_NUMBER")]?.props?.children,
  },
});