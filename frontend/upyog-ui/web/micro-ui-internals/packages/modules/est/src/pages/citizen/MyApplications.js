import React from "react";
import { Header, Card, KeyNote } from "@upyog/digit-ui-react-components";
import { Link } from "react-router-dom";

const MyApplications = () => {
  const applications = [
    {
      applicationNumber: "EST-APP-001",
      assetNumber: "EST-001-A",
      applicantName: "John Doe",
      status: "Approved"
    },
    {
      applicationNumber: "EST-APP-002",
      assetNumber: "EST-002-B", 
      applicantName: "Jane Smith",
      status: "Pending"
    }
  ];

  return (
    <div>
      <Header>My Applications</Header>
      {applications.map((app, index) => (
        <Card key={index}>
          <KeyNote keyValue="Application Number" note={app.applicationNumber} />
          <KeyNote keyValue="Asset Number" note={app.assetNumber} />
          <KeyNote keyValue="Applicant Name" note={app.applicantName} />
          <KeyNote keyValue="Status" note={app.status} />
        </Card>
      ))}
    </div>
  );
};

export default MyApplications;
