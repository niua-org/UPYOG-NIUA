import { Header, Loader, TextInput, SubmitBar, CardLabel, Card, KeyNote } from "@upyog/digit-ui-react-components";
import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";

const EstateApplication = ({ application, tenantId, buttonLabel }) => {
  const handlePayment = (appNumber, amount) => {
    alert(`Payment for ${appNumber} - Amount: ₹${amount}`);
  };

  return (
    <Card style={{ marginBottom: "20px" }}>
      <KeyNote keyValue="Application Number" note={application.applicationNumber} />
      <KeyNote keyValue="Asset Number" note={application.assetNumber} />
      <KeyNote keyValue="Applicant Name" note={application.applicantName} />
      <KeyNote keyValue="Status" note={application.status} />
      
      {application.status === "Approved" && (
        <>
          <KeyNote keyValue="Rent Due" note={`₹${application.rentDue}`} />
          <KeyNote keyValue="Due Date" note={application.dueDate} />
          <SubmitBar 
            label="Pay Rent" 
            onSubmit={() => handlePayment(application.applicationNumber, application.rentDue)}
          />
        </>
      )}
      
      <div style={{ marginTop: "10px" }}>
        <Link to={`/upyog-ui/citizen/est/application/${application.acknowldgementNumber}/${tenantId}`}>
          {buttonLabel || "View Details"}
        </Link>
      </div>
    </Card>
  );
};

export const ESTMyApplications = () => {
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const [applicationsList, setApplicationsList] = useState([]);

  // Mock data
  const mockApplications = [
    {
      applicationNumber: "EST-APP-001",
      assetNumber: "EST-001-A",
      applicantName: "John Doe",
      status: "Approved",
      rentDue: "5000",
      dueDate: "2024-01-15",
      acknowldgementNumber: "EST-ACK-001"
    },
    {
      applicationNumber: "EST-APP-002",
      assetNumber: "EST-002-B", 
      applicantName: "Jane Smith",
      status: "Pending",
      rentDue: "4000",
      dueDate: "2024-01-20",
      acknowldgementNumber: "EST-ACK-002"
    }
  ];

  useEffect(() => {
    setApplicationsList(mockApplications);
  }, []);

  return (
    <React.Fragment>
      <Header>My Applications ({applicationsList.length})</Header>

      <div>
        {applicationsList?.length > 0 &&
          applicationsList.map((application, index) => (
            <div key={index}>
              <EstateApplication 
                application={application} 
                tenantId={tenantId} 
                buttonLabel="Track Application" 
              />
            </div>
          ))}
        {!applicationsList?.length > 0 && <p style={{ marginLeft: "16px", marginTop: "16px" }}>No applications found</p>}
      </div>
    </React.Fragment>
  );
};
