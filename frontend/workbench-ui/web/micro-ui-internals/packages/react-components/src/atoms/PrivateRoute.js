import React from "react";
import { Route, Navigate } from "react-router-dom";

export const PrivateRoute = ({ component: Component, roles, ...rest }) => {
  return (
    <Route
      {...rest}
      element={(props) => {
        const user = Digit.UserService.getUser();
        const userType = Digit.UserService.getType();
        function getLoginRedirectionLink (){
          if(userType === "employee"){
            return `/workbench-ui/employee/user/language-selection`
          }
          else{
            return `/workbench-ui/citizen/login`
          }
        }
        if (!user || !user.access_token) {
          // not logged in so redirect to login page with the return url
          return <Navigate to={{ pathname: getLoginRedirectionLink(), state: { from: props.location.pathname + props.location.search } }} />;
        }

        // logged in so return component
        return <Component {...props} />;
      }}
    />
  );
};
