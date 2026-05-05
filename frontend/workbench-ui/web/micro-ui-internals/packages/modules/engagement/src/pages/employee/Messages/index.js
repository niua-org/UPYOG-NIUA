import React from "react"
import { Routes, Route } from "react-router-dom"
import { PrivateRoute } from "@upyog/workbench-ui-react-components"
import Inbox from "./Inbox"
import NewMessage from "./NewMessage"
import Response from "./NewMessage/Response"
import EditMessage from "./EditMessage"
import MessageDetails from "./MessageDetails"
import DocumentDetails from "../../../components/Messages/DocumentDetails"

const Messages = ({match:{path} = {}, tenants, parentRoute}) => {
    return <Routes>
        <Route path={`${path}/create`} element={<NewMessage />} />
        <Route path={`${path}/inbox/create`} element={<NewMessage />} />
        <Route path={`${path}/inbox/details/:id`} element={<DocumentDetails />} />
        <Route path={`${path}/inbox/edit/:id`} element={<EditMessage />} />
        <Route path={`${path}/inbox`} element={<Inbox tenants={tenants} parentRoute={parentRoute} />} />
        <Route path={`${path}/response`} element={<Response />} />
    </Routes>
}

export default Messages