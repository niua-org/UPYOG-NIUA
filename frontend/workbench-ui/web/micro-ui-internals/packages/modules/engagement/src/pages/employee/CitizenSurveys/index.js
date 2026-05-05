import React from "react"
import { Routes, Route } from "react-router-dom"
import { PrivateRoute } from "@upyog/workbench-ui-react-components"
import Inbox from "./Inbox"
import NewSurvey from "./NewSurvey"
import CreateResponse from "./responses/create"
import UpdateResponse from './responses/update'
import DeleteResponse from "./responses/delete"
//import EditSurvey from "./EditSurvey"
import SurveyDetails from "./SurveyDetails"
import SurveyResults from "./SurveyResults"

const Surveys = ({tenants, parentRoute}) => {
    // match:{path} prop removed — In v6 routes not get props 

    return <Routes>
            {/* Route + element prop + relative paths */}
            <Route path="inbox/create" element={<PrivateRoute element={<NewSurvey />} />} />
            <Route path="create" element={<PrivateRoute element={<NewSurvey />} />} />
            <Route path="inbox/details/:id" element={<PrivateRoute element={<SurveyDetails />} />} />
            <Route path="inbox/results/:id" element={<PrivateRoute element={<SurveyResults />} />} />
            <Route path="inbox" element={<PrivateRoute element={<Inbox tenants={tenants} parentRoute={parentRoute} />} />} />
            <Route path="create-response" element={<PrivateRoute element={<CreateResponse />} />} />
            <Route path="update-response" element={<PrivateRoute element={<UpdateResponse />} />} />
            <Route path="delete-response" element={<PrivateRoute element={<DeleteResponse />} />} />
        </Routes>

}

export default Surveys