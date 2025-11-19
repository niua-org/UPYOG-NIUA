import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {div} from "@upyog/digit-ui-react-components";
import NewRegistration from "../../PageComponents/ESTNEWRegistration";

const NewRegistrationPage = () => {
  return (
    <div className="page-wrapper">
       {<NewRegistration />}
    </div>
  );
};

export default NewRegistrationPage;