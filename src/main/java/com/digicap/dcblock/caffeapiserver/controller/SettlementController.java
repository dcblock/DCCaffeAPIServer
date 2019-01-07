package com.digicap.dcblock.caffeapiserver.controller;

import com.digicap.dcblock.caffeapiserver.CaffeApiServerApplicationConstants;
import com.digicap.dcblock.caffeapiserver.dto.SettlementReportDto;
import com.digicap.dcblock.caffeapiserver.dto.SettlementReportGuestsDto;
import com.digicap.dcblock.caffeapiserver.dto.SettlementReportPageDto;
import com.digicap.dcblock.caffeapiserver.dto.SettlementUserReportDto;
import com.digicap.dcblock.caffeapiserver.service.SettlementService;
import com.google.common.base.Preconditions;
import java.util.LinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;

/**
 * 정산처리 Controller.
 */
@RestController
public class SettlementController implements CaffeApiServerApplicationConstants {

    private SettlementService settlementService;

    // --------------------------------------------------------------------------------------------
    // Constructor

    @Autowired
    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * 기간의 모든 사용자 구매 정산 목록
     *
     * @param before
     * @param after
     * @return
     */
    @GetMapping("/api/caffe/settlement/reports")
    LinkedList<SettlementReportDto> getSettlements(
            @RequestParam(value = "before", defaultValue = "0") long before,
            @RequestParam(value = "after", defaultValue = "0") long after,
            @RequestParam(value = "company", defaultValue = "") String company
//            ,
//            @RequestParam(value = "page", defaultValue = "0") int page,
//            @RequestParam(value = "per_page", defaultValue = "0") int perPage
    ) {
        // Check Argument
        Preconditions.checkArgument(before > 0, "before is empty");
        Preconditions.checkArgument(after > 0, "after is empty");
        if (!company.isEmpty()) { // empty 경우는 무시.
            Preconditions.checkArgument(company.toLowerCase().equals(COMPANY_DIGICAP)
                            || company.toLowerCase().equals(COMPANY_COVISION),
                    "unknown company(%s)", company);
        }

        // unix time to timestamp
        Timestamp b = new Timestamp(before * 1_000L);
        Timestamp a = new Timestamp(after * 1_000L);

        Preconditions.checkArgument(!b.after(a), "before(%s) is bigger then after(%s)", b.toString(), a.toString());

//        if (page == 0) {
            LinkedList<SettlementReportDto> results = settlementService.getReports(b, a, company);
            return results;
//        } else {
//            Preconditions.checkArgument( 10 <= perPage && perPage <= 50, "size is %s. size is 10 ~ 50", perPage);
//            Preconditions.checkArgument( page > 0, "page is %s. page start is 1", page);
//
//            SettlementReportPageDto result = settlementService.getReports(b, a, "digicap", page, perPage);
//            return result;
//        }
    }

    /**
     * 사용자의 기간별 대금을 정산.
     *
     * @param before
     * @param after
     * @param userRecordIndex
     * @return
     */
    @GetMapping("/api/caffe/settlement/report")
    SettlementUserReportDto getPurchases(@RequestParam(value = "before", defaultValue = "0") long before,
                                         @RequestParam(value = "after", defaultValue = "0") long after,
                                         @RequestParam(value = "user_index", defaultValue = "0") long userRecordIndex) {
        // Check Argument
        Preconditions.checkArgument(before > 0, "before is empty");
        Preconditions.checkArgument(after > 0, "after is empty");
        Preconditions.checkArgument(userRecordIndex > 0, "invalid user_index(%d)", userRecordIndex);

        // unix time to timestamp
        Timestamp b = new Timestamp(before * 1_000L);
        Timestamp a = new Timestamp(after * 1_000L);

        Preconditions.checkArgument(!b.after(a), "before(%s) is bigger then after(%s)", b.toString(), a.toString());

        SettlementUserReportDto result = settlementService.getReportByRecordIndex(b, a, userRecordIndex);
        return result;
    }

    /**
     * 기간동안의 손님결제된 금액 정산.
     *
     * @param before
     * @param after
     * @param userRecordIndex
     * @return
     */
    @GetMapping("/api/caffe/settlement/guests")
    SettlementReportGuestsDto getSettlementsForGuests(@RequestParam(value = "before", defaultValue = "0") long before,
                                                      @RequestParam(value = "after", defaultValue = "0") long after,
                                                      @RequestParam(value = "user_index", defaultValue = "0") long userRecordIndex) {
        // Check Argument
        Preconditions.checkArgument(before > 0, "before is empty");
        Preconditions.checkArgument(after > 0, "after is empty");
        if (userRecordIndex > 0) {
            Preconditions.checkArgument(userRecordIndex > 0, "invalid user_index(%s)", userRecordIndex);
        }

        // unix time to timestamp
        Timestamp b = new Timestamp(before * 1_000L);
        Timestamp a = new Timestamp(after * 1_000L);

        Preconditions.checkArgument(!b.after(a), "before(%s) is bigger then after(%s)", b.toString(), a.toString());

        // Get Result
        SettlementReportGuestsDto result = settlementService.getReportForGuests(b, a, userRecordIndex);
        return result;
    }

    // ---------------------------------------------------------------------------------------------
    // Private Methods
}
