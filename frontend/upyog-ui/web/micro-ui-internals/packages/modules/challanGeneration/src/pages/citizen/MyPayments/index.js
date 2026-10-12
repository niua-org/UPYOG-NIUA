import React, { useState, useEffect } from "react";
import { Header, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import ChallanPayments from "./ChallanPayments";

export const ChallanMyPayments = () => {
  const { t } = useTranslation();
  const userInfo = Digit.UserService.getUser();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  const filters = {
    mobileNumber: userInfo?.info?.mobileNumber,
  };

  const useSearch =
    Digit.Hooks?.challangeneration?.useChallanGenerationSearch ||
    Digit.Hooks?.challanGeneration?.useChallanGenerationSearch;

  const searchResult = useSearch
    ? useSearch({ tenantId, filters }, { enabled: !!(tenantId && filters.mobileNumber) })
    : { isLoading: false, data: null };

  const { isLoading, data } = searchResult || {};
  const paymentsList = data?.challans || [];

  const [receiptsMap, setReceiptsMap] = useState({});
  const [isReceiptsLoading, setIsReceiptsLoading] = useState(false);

  const consumerCodes = paymentsList
    .map((c) => c.challanNo || c.consumerCode)
    .filter(Boolean)
    .join(",");

  useEffect(() => {
    let isMounted = true;
    const fetchAllReceipts = async () => {
      if (!consumerCodes) {
        setIsReceiptsLoading(false);
        return;
      }
      setIsReceiptsLoading(true);
      try {
        const res = await Digit.PaymentService.getReciept(tenantId, "Challan_Generation", {
          consumerCodes: consumerCodes,
        });
        if (isMounted) {
          const map = {};
          if (res?.Payments && Array.isArray(res.Payments)) {
            res.Payments.forEach((payment) => {
              const code =
                payment?.paymentDetails?.[0]?.bill?.consumerCode ||
                payment?.paymentDetails?.[0]?.bill?.billDetails?.[0]?.consumerCode ||
                payment?.consumerCode;
              if (code) {
                map[code] = payment;
              }
            });
          }
          setReceiptsMap(map);
          setIsReceiptsLoading(false);
        }
      } catch (err) {
        if (isMounted) setIsReceiptsLoading(false);
      }
    };

    if (paymentsList.length > 0) {
      fetchAllReceipts();
    } else if (!isLoading) {
      setIsReceiptsLoading(false);
    }

    return () => {
      isMounted = false;
    };
  }, [consumerCodes, tenantId, isLoading]);

  if (isLoading || (paymentsList.length > 0 && isReceiptsLoading && Object.keys(receiptsMap).length === 0)) {
    return <Loader />;
  }

  return (
    <React.Fragment>
      <Header>{`${t("CHALLAN_MY_PAYMENTS_HEADER", t("CS_TITLE_MY_PAYMENTS"))} ${
        paymentsList ? `(${paymentsList.length})` : ""
      }`}</Header>
      <div>
        {paymentsList?.length > 0 ? (
          paymentsList.map((payment, index) => {
            const challanNo = payment?.challanNo || payment?.consumerCode;
            return (
              <div key={challanNo || index}>
                <ChallanPayments payment={payment} receiptData={receiptsMap[challanNo]} />
              </div>
            );
          })
        ) : (
          <p style={{ margin: "24px 0", color: "#505A5F" }}>
            {t("CHALLAN_NO_PAYMENTS_FOUND", t("CS_MYAPPLICATIONS_NO_APPLICATION"))}
          </p>
        )}
      </div>
    </React.Fragment>
  );
};

export default ChallanMyPayments;
