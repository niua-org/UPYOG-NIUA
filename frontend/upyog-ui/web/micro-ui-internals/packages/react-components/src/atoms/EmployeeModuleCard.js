import React, { Fragment } from "react";
import { ArrowRightInbox } from "./svgindex";
import { Link } from "react-router-dom";

// const EmployeeModuleCard = ({ Icon, moduleName, kpis = [], links = [], isCitizen = false, className, styles, longModuleName=false, FsmHideCount }) => {
//   return (
//     <div className={className ? className : "employeeCard customEmployeeCard card-home home-action-cards"} style={styles ? styles : {}}>
//       <div className="complaint-links-container">
//         <div className="header" style={isCitizen ? { padding: "0px" } : longModuleName ? {alignItems:"flex-start"}:{}}>
//           <span className="text removeHeight">{moduleName}</span>
//           <span className="logo removeBorderRadiusLogo">{Icon}</span>
//         </div>
//         <div className="body" style={{ margin: "0px", padding: "0px" }}>
//           {kpis.length !== 0 && (
//             <div className="flex-fit" style={isCitizen ? { paddingLeft: "17px" } : {}}>
//               {kpis.map(({ count, label, link }, index) => (
//                 <div className="card-count" key={index}>
//                   <div>
//                     <span>{count ? count : count == 0 ? 0 : "-"}</span>
//                   </div>
//                   <div>
//                     {link ? (
//                       <Link to={link} className="employeeTotalLink">
//                         {label}
//                       </Link>
//                     ) : null}
//                   </div>
//                 </div>
//               ))}
//             </div>
//           )}
//           <div className="links-wrapper" style={{ width: "80%" }}>
//             {links.map(({ count, label, link }, index) => (
//               <span className="link" key={index}>
//                 {link ? <Link to={link}>{label}</Link> : null}
//                 {count ? (
//                   <Fragment>
//                     {FsmHideCount ? null : <span className={"inbox-total"}>{count || "-"}</span>}
//                     <Link to={link}>
//                       <ArrowRightInbox />
//                     </Link>
//                   </Fragment>
//                 ) : null}
//               </span>
//             ))}
//           </div>
//         </div>
//       </div>
//     </div>
//   );
// };
const EmployeeModuleCard = ({ Icon, moduleName, kpis = [], links = [], isCitizen = false, className, styles, FsmHideCount }) => {
  return (
    <div className={className ? "employeeCard card-home customEmployeeCard" : "employeeCard card-home customEmployeeCard"} style={className ? {} : styles}>
      <div className="employeeCustomCard" style={{ width: "100%", height: "85%", position: "relative" }}>
        <span
          className="text-employee-card"
          style={{ width: "calc(100% - 80px)", boxSizing: "border-box", lineHeight: "1.2" }}
        >
          {moduleName}
        </span>
        <span className="logo-removeBorderRadiusLogo" style={{ position: "absolute", right: "10%", top: "10%" }}>{Icon}</span>
        <div className="employee-card-banner">
          <div className="body" style={{ margin: "0px", padding: "0px" }}>
            <div className="flex" style={{ flexDirection: "column" }}>
              <div className="flex">
            <div style={{ width: "30%", height: "50px" }}><span className="icon-banner-employee" style={{ position: "absolute", left: "10%", top: "10%", borderRadius: "5px", boxShadow: "5px 5px 5px 0px #e3e4e3" }}>{Icon}</span></div>
            
            <div style={{width:"70%"}}>
            {kpis.length !== 0 && (
              <div className="flex-fit" style={isCitizen ? { paddingLeft: "17px" } : {}}>

                {kpis.map(({ count, label, link }, index) => (
                  <div className="card-count flex" key={index} style={{ width: "100%", flexDirection: "column" }}>
                    {/*  */}
                    <div className="flex" style={{ marginLeft: "auto", flexDirection: "column-reverse", width: "100%" }}>

                      <div className="text-center">
                        {link ? (
                          <Link to={link} className="employeeTotalLink">
                            {label}
                          </Link>
                        ) : null}
                    </div>
                      <div className="text-center">
                        <span className="text-lg font-bold" style={{ color: "#ae1e28", fontFamily: "sans-serif" }}>{count || "-"}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
            </div>
            </div>
            <div>
            <div className="links-wrapper flex" style={{ width: "100%", fontSize: "0.8rem", paddingLeft: "10px", flexWrap:"wrap", flexDirection:"row", paddingTop:"10px" }}>
              {links.map(({ count, label, link }, index) => (
                <div className="link flex" key={index} style={{ paddingLeft: "5px", color: "#a1a5b7" }}>
                  {link ? <div className="flex"> <Link to={link}> {label} </Link>  <span>|</span> </div>: null}
                </div>

              ))}
            </div>
          </div>
          </div>
          </div>
        </div>
      </div>
      
      <div>
      </div>
    </div>
  );
};
const ModuleCardFullWidth = ({ moduleName,  links = [], isCitizen = false, className, styles, headerStyle, subHeader, subHeaderLink }) => {
  return (
    <div className={className ? className : "employeeCard card-home customEmployeeCard home-action-cards"} style={styles ? styles : {}}>
      <div className="complaint-links-container p-sm">
        <div className="header" style={isCitizen ? { padding: "0px" } : headerStyle}>
          <span className="text removeHeight">{moduleName}</span>
          <span className="link">
            <a href={subHeaderLink}>
              <span className="inbox-total flex items-center text-primary-main font-bold">
                {subHeader || "-"}
                <span className="ml-sm">
                  {" "}
                  <ArrowRightInbox />
                </span>
              </span>
            </a>
          </span>
        </div>
        <div className="body m-0 p-0">
          <div className="links-wrapper flex w-full flex-wrap">
            {links.map(({ count, label, link }, index) => (
              <span className="link full-employee-card-link" key={index}>
                {link ? (link?.includes('upyog-ui/')?<Link to={link}>{label}</Link>:<a href={link}>{label}</a>) : null}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export { EmployeeModuleCard, ModuleCardFullWidth };
