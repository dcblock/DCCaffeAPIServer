package com.digicap.dcblock.caffeapiserver.service;

import com.digicap.dcblock.caffeapiserver.dto.*;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public interface PurchaseService {

  ReceiptIdDto getReceiptId(String rfid);

  PurchasedDto requestPurchases(int receiptId, int type, List<LinkedHashMap<String, Object>> purchases);

  List<PurchaseDto> cancelPurchases(int receiptId);

  List<PurchaseDto> cancelApprovalPurchases(int receiptId, Timestamp purchaseDate);

  LinkedList<PurchaseOldDto> getPurchases(PurchaseDto purchaseDto, Timestamp before, Timestamp after);

  PurchaseBalanceDto getBalanceByRfid(String rfid, Timestamp before, Timestamp after);

  LinkedList<PurchaseSearchDto> getPurchasesBySearch(Timestamp before, Timestamp after, int filter, int recordIndex);
}
