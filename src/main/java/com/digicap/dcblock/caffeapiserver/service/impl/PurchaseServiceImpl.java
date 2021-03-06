package com.digicap.dcblock.caffeapiserver.service.impl;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.digicap.dcblock.caffeapiserver.CaffeApiServerApplicationConstants;
import com.digicap.dcblock.caffeapiserver.dao.ReceiptIdDao;
import com.digicap.dcblock.caffeapiserver.dto.MenuDto;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseBalanceDto;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseDto;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseNewDto;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseSearchDto;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseSearchPageDto;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseVo;
import com.digicap.dcblock.caffeapiserver.dto.PurchaseWhere;
import com.digicap.dcblock.caffeapiserver.dto.PurchasedDto;
import com.digicap.dcblock.caffeapiserver.dto.ReceiptIdDto;
import com.digicap.dcblock.caffeapiserver.dto.ReceiptIdVo;
import com.digicap.dcblock.caffeapiserver.dto.UserDto;
import com.digicap.dcblock.caffeapiserver.exception.ExpiredTimeException;
import com.digicap.dcblock.caffeapiserver.exception.InvalidParameterException;
import com.digicap.dcblock.caffeapiserver.exception.NotFindException;
import com.digicap.dcblock.caffeapiserver.exception.UnknownException;
import com.digicap.dcblock.caffeapiserver.proxy.AdminServer;
import com.digicap.dcblock.caffeapiserver.service.MenuService;
import com.digicap.dcblock.caffeapiserver.service.PurchaseService;
import com.digicap.dcblock.caffeapiserver.store.MenuMapper;
import com.digicap.dcblock.caffeapiserver.store.PurchaseMapper;
import com.digicap.dcblock.caffeapiserver.type.OptSize;
import com.digicap.dcblock.caffeapiserver.type.OptType;
import com.digicap.dcblock.caffeapiserver.type.PurchaseType;
import com.digicap.dcblock.caffeapiserver.util.TimeFormat;

import lombok.extern.slf4j.Slf4j;

@Service
@Primary
@Slf4j
public class PurchaseServiceImpl implements PurchaseService, CaffeApiServerApplicationConstants {

    private PurchaseMapper purchaseMapper;

    private ReceiptIdDao receiptIdDao;

    private MenuMapper menuMapper;

    private MenuService menuService;

    @Value("${api-version}")
    private String apiVersion;

    @Value("${admin-server}")
    private String adminServer;

    @Value("${cancel-able-minute:10}")
    private int cancelTime;

    @Autowired
    public PurchaseServiceImpl(PurchaseMapper purchaseMapper, ReceiptIdDao receiptIdDao,
                               MenuMapper menuMapper, MenuService menuService) {
        this.purchaseMapper = purchaseMapper;

        this.receiptIdDao = receiptIdDao;

        this.menuMapper = menuMapper;

        this.menuService = menuService;
    }

    @Override
    public ReceiptIdDto getReceiptId(String rfid) throws UnknownException, MyBatisSystemException, NotFindException {
        UserDto userDto = null;

        try {
            userDto = new AdminServer(adminServer, apiVersion).getUserByRfid(rfid);
        } catch (Exception e) {
            throw new UnknownException(String.format("Admin Server: %s", e.getMessage()));
        }

        if (userDto == null) {
            throw new NotFindException("not find user using rfid");
        }

        // instance ReceiptIdVo
        ReceiptIdDto receiptId = new ReceiptIdDto(userDto);

        // receipts table에 insert
        int result = receiptIdDao.insertByReceipt(receiptId);
        if (result == 0) {
            throw new UnknownException("db error");
        }

        return receiptId;
    }

    /**
     * 구매 요청을 처리.
     *
     * @param receiptId  구매하려는 사용자가 발급받은 영수증 ID.
     * @param _purchases 구매목록.
     */
    @Override
    public PurchasedDto requestPurchases(int receiptId, PurchaseType type, List<LinkedHashMap<String, Object>> _purchases)
            throws MyBatisSystemException, NotFindException, InvalidParameterException, UnknownException {
        // parameter 확인
        ReceiptIdVo receiptIdVo = receiptIdDao.selectByReceipt(receiptId);
        if (receiptIdVo == null) {
            throw new NotFindException("not find receiptId");
        }

        // receiptId는 구매 API 성공과 상관없이 일회용.
        try {
            receiptIdDao.deleteByReceiptId(receiptId);
        } catch (MyBatisSystemException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            // 로그만 남기고 프로세스 동작.
        }

        // hashmap to instance
        LinkedList<PurchaseDto> purchases = toPurchaseDtos(_purchases, receiptIdVo.getEmail(), receiptIdVo.getCompany());

        LinkedHashMap<Integer, LinkedList<MenuDto>> menusInCategory = menuService.getAllMenusUsingCode();

        // 구매요청한 카테고리, 메뉴 확인
        for (int i = 0; i < purchases.size(); i++) {
            int category = purchases.get(i).getCategory();
            int code = purchases.get(i).getCode();
            boolean bExist = menuMapper.existCode(code, category);
            if (!bExist) {
                throw new InvalidParameterException("unknown category: " + category);
            }

            for (MenuDto menu : menusInCategory.get(category)) {
                if (menu.getCode() == code) {
                    // 사용자 정보.
                    purchases.get(i).setName(receiptIdVo.getName());
                    purchases.get(i).setUser_record_index(receiptIdVo.getUserRecordIndex());

                    // 구매 정보.
                    purchases.get(i).setPrice(menu.getPrice());
                    purchases.get(i).setMenu_name_kr(menu.getName_kr());
                    purchases.get(i).setReceipt_id(receiptId);

                    // DC 가격
                    String company = receiptIdVo.getCompany();
                    purchases.get(i).setDc_price(menu.getDiscounts().getOrDefault(company, 0));
                }
            }
        }

        // Insert Purchases Table.
        for (PurchaseDto p : purchases) {
            try {
                p.setReceipt_status(RECEIPT_STATUS_PURCHASE);
                p.setPurchase_type(type.ordinal());

                int result = purchaseMapper.insertPurchase(p);
                if (result == 0) {
                    // TODO receipt id db delete
                    throw new UnknownException("DB Insert Error.");
                }
            } catch (Exception e) {
                // TODO receipt id db delete
                throw new UnknownException(e.getMessage());
            }
        }

        PurchasedDto purchasedDto = new PurchasedDto();
        purchasedDto.setTotal_price(calcTotalPrice(purchases));
        purchasedDto.setTotal_dc_price(calcTotalDcPrice(purchases));
        purchasedDto.setPurchased_date(new TimeFormat().getCurrent());
        return purchasedDto;
    }

    /**
     * 구매된 목록 중에서 취소 요청
     */
    @Override
    public LinkedList<PurchaseVo> cancelPurchases(int receiptId, String rfid) throws MyBatisSystemException,
            NotFindException, ExpiredTimeException {
        // find User by rfid
        UserDto userDto = null;

        try {
            userDto = new AdminServer(adminServer, apiVersion).getUserByRfid(rfid);
        } catch (Exception e) {
            throw new UnknownException(String.format("Admin Server: %s", e.getMessage()));
        }

        if (userDto == null) {
            throw new NotFindException(String.format("not find user by rfid(%s)", rfid));
        }

        // 오늘 receiptId로 구매된 결과가 있는지 확인
        LinkedList<Timestamp> updateDatePurchases = purchaseMapper.selectByReceiptId(receiptId,
                userDto.getIndex());
        if (updateDatePurchases == null || updateDatePurchases.size() == 0) {
            throw new NotFindException("not find purchases by receiptId");
        }

        // 구매 취소 가능한 시간 확인. 10분
        if (!enablePurchaseCancel(updateDatePurchases.getFirst())) {
            throw new ExpiredTimeException("expired time for cancel purchase");
        }

        LinkedList<PurchaseVo> r = purchaseMapper.updateReceiptCancelStatus(receiptId);
        if (r == null || r.size() == 0) {
            throw new NotFindException("not find purchases by receiptId");
        }

        return r;
    }

    /**
     * 구매취소 요청을 승인
     *
     * @param receiptId receipt id
     */
    @Override
    public List<PurchaseVo> cancelApprovalPurchases(int receiptId, Timestamp today)
            throws MyBatisSystemException, NotFindException {
        // Get Tomorrow
        Timestamp tomorrow = new TimeFormat().toTomorrow(today);

        if (!purchaseMapper.existReceiptId(receiptId, today, tomorrow)) {
            throw new NotFindException(String.format("not find receipt_id(%s)", receiptId));
        }

        LinkedList<PurchaseVo> results = purchaseMapper.updateReceiptCancelApprovalStatus(receiptId, today, tomorrow);
        if (results == null || results.size() == 0) {
            throw new NotFindException(String.format("not find cancel' purchase using receipt_id(%s)", receiptId));
        }

        return results;
    }

    @Override
    public LinkedList<PurchaseVo> getPurchases(PurchaseDto purchaseDto, Timestamp from, Timestamp to)
            throws MyBatisSystemException {
        LinkedList<PurchaseVo> purchases = purchaseMapper.selectAllUser(from, to, purchaseDto.getUser_record_index(), "");
        return purchases;
    }

    @Override
    public PurchaseBalanceDto getBalanceByRfid(String rfid, Timestamp before, Timestamp after)
            throws MyBatisSystemException, NotFindException {

        // Get user from AdminServer.
        UserDto userDto = null;

        try {
            userDto = new AdminServer(adminServer, apiVersion).getUserByRfid(rfid);
            if (userDto == null) {
                throw new NotFindException(String.format("not find user using rfid(%s)", rfid));
            }
        } catch (NotFindException e) {
            throw e;
        } catch (Exception e) {
            throw new UnknownException(String.format("Admin Server: %s", e.getMessage()));
        }

        // Set Where for Query
        PurchaseWhere w = PurchaseWhere.builder()
                .before(before)
                .after(after)
                .userRecordIndex(userDto.getIndex())
                .purchaseType(PURCHASE_TYPE_DEFAULT)
                .receiptStatus(RECEIPT_STATUS_DEFAULT)
                .build();
        
        // Execute Query.
        HashMap<String, Long> balances = purchaseMapper.selectBalanceAccounts(w);

        // Result
        PurchaseBalanceDto balanceDto = new PurchaseBalanceDto();
        if (balances != null) {
            balanceDto.setTotal_price(balances.getOrDefault("balance", (long) 0));
            balanceDto.setTotal_dc_price(balances.getOrDefault("dcbalance", (long) 0));
        } else {
            balanceDto.setTotal_price(0);
            balanceDto.setTotal_dc_price(0);
        }

        balanceDto.setName(userDto.getName());
        
        return balanceDto;
    }

    @Override
    public PurchaseSearchPageDto getPurchasesBySearch(PurchaseWhere w) {
        /* userRecordIndex 는 filter 가 -1인 경우에만 사용 */

        PurchaseSearchPageDto result = new PurchaseSearchPageDto();

        LinkedList<PurchaseSearchDto> results = new LinkedList<>();

        if (w.getFilter() == 3) { // 3 is cancel and canceled
            // Get cancel, canceled
            // ORDER BY update DESC
            LinkedList<PurchaseVo> r = purchaseMapper.selectAllCancel(w);
            if (r == null) {
                throw new NotFindException("not find purchases");
            }

            // 정의된 응답으로 변경.
            for (PurchaseVo p : r) {
                PurchaseSearchDto ps = new PurchaseSearchDto(p);
                results.add(ps);
            }

            try {
                int totalCount = purchaseMapper.selectAllCancelCount(w);
                if (totalCount % w.getPerPage() > 0) {
                    totalCount /= w.getPerPage();
                    totalCount++;
                } else {
                   totalCount /= w.getPerPage();
                }
                result.setTotalPages(totalCount);
            } catch (Exception e) {
                throw e;
            }
        } else if (w.getFilter() == -1) { // -1 is all
            LinkedList<PurchaseVo> r = purchaseMapper.selectSearchBy(w);
            if (r == null) {
                throw new NotFindException(String.format("not find purchases for user(%s)", w.getUserRecordIndex()));
            }

            // 정의된 응답으로 변경.
            for (PurchaseVo p : r) {
                PurchaseSearchDto ps = new PurchaseSearchDto(p);
                results.add(ps);
            }

            try {
                int totalCount = purchaseMapper.selectCount(w);
                int totalPage = totalCount / w.getPerPage();
                if (totalCount % w.getPerPage() > 0) {
                    totalCount = totalPage++;
                } else {
                    totalCount = totalPage;
                }
                result.setTotalPages(totalCount);
            } catch (Exception e) {
                throw e;
            }
        }

        result.setList(results);

        return result;
    }

    // -------------------------------------------------------------------------
    // Private Methods

    /**
     * 숫자를 네자리 문자열로 변경. 공백은 0으로 채움.
     *
     * @param number 숫자.
     */
    private String insertZeroString(int number) {
        int length = String.valueOf(number).length();

        String value = String.valueOf(number);

        if (length == 5) {
            return "0" + value;
        } else if (length == 4) {
            return "00" + value;
        } else if (length == 3) {
            return "000" + value;
        } else if (length == 2) {
            return "0000" + value;
        } else if (length == 1) {
            return "00000" + value;
        } else {
            return value;
        }
    }

    private LinkedList<PurchaseDto> toPurchaseDtos(List<LinkedHashMap<String, Object>> purchases,
                                                   String email, String company) {
        LinkedList<PurchaseDto> results = new LinkedList<>();

        for (LinkedHashMap<String, Object> p : purchases) {
            PurchaseDto pp = new PurchaseDto();
            pp.setCategory(Integer.valueOf(p.getOrDefault("category", -1).toString()));
            pp.setCode(Integer.valueOf(p.getOrDefault("code", -1).toString()));
            pp.setCount(Integer.valueOf(p.getOrDefault("count", -1).toString()));
            pp.setEmail(email);
            pp.setCompany(company);

            pp.setOpt_size(OptSize.valueOf(p.getOrDefault("size", OptSize.REGULAR).toString()));
            pp.setOpt_type(OptType.valueOf(p.getOrDefault("type", OptType.BOTH).toString()));
//            String size = p.getOrDefault("size", -1).toString().toUpperCase();
//            switch (size) {
//                case OPT_SIZE_REGULAR:
//                    pp.setOpt_size(0);
//                    break;
//                case OPT_SIZE_SMALL:
//                    pp.setOpt_size(1);
//                    break;
//                default:
//                    throw new InvalidParameterException(String.format("unknown size(%s)", size));
//            }

//            String type = p.getOrDefault("type", -1).toString().toUpperCase();
//            switch (type) {
//                case OPT_TYPE_HOT:
//                    pp.setOpt_type(0);
//                    break;
//                case OPT_TYPE_ICED:
//                    pp.setOpt_type(1);
//                    break;
//                case OPT_TYPE_BOTH:
//                    pp.setOpt_type(2);
//                default:
//                    throw new InvalidParameterException(String.format("unknown type(%s)", type));
//            }

            results.add(pp);
        }

        return results;
    }

    /**
     * 구매목록에서 총 구매비용을 계산
     *
     * @param purchases purchase list
     * @return total price
     */
    private int calcTotalPrice(LinkedList<PurchaseDto> purchases) {
        int total = 0;

        for (PurchaseDto p : purchases) {
            total += (p.getPrice() * p.getCount());
        }

        return total;
    }

    /**
     * 구매목록에서 총 할인비용을 계산
     *
     * @param purchases purchase list
     * @return total price
     */
    private int calcTotalDcPrice(LinkedList<PurchaseDto> purchases) {
        int total = 0;

        for (PurchaseDto p : purchases) {
            total += (p.getDc_price() * p.getCount());
        }

        return total;
    }

    /**
     * 구매취소 가능 시간을 확인
     *
     * @param timestamp purchase timestamp
     */
    private boolean enablePurchaseCancel(Timestamp timestamp) {
        LocalTime startTime = timestamp.toLocalDateTime().toLocalTime();
        LocalTime endTime = LocalTime.now();

        // 구매시간과 현재시간 차이가 10분 이하 인 경우에만 유효.
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes >= 0 && minutes <= cancelTime) {
            // TODO 23:55에 구매한걸 00:03분에 취소할 수 없음.
            return true;
        }

        return false;
    }

    /**
     * 일정기간 동안의 목록을 Get.
     *
     * @param from
     * @param to
     * @param purchases
     * @return
     */
    private LinkedList<PurchaseNewDto> getPurchasePeriod(Timestamp from, Timestamp to, LinkedList<PurchaseNewDto> purchases) {
        LinkedList<PurchaseNewDto> r = new LinkedList<>();

        Iterator<PurchaseNewDto> itr = purchases.iterator();
        while (itr.hasNext()) {
            PurchaseNewDto p = itr.next();

            Timestamp t = p.getUpdate_date();
            if (t.after(from) && t.before(to)) { // from < t < to
                r.add(p);
                itr.remove();
            }
        }

        return r;
    }

    /**
     * receiptID 기준으로 역순 정렬
     *
     * @param purchases
     */
    private void sortDescByReceiptId(LinkedList<PurchaseNewDto> purchases) {
        log.debug("");
        // ReceiptID Order DESC
        purchases.sort(new Comparator<PurchaseNewDto>() {

            @Override
            public int compare(PurchaseNewDto o1, PurchaseNewDto o2) {
                return o1.getReceipt_id() - o2.getReceipt_id();
            }
        });

        log.debug("");
    }

    /**
     * ReceiptID 기준으로 그룹화.
     *
     * @param purchases
     * @return
     */
    private LinkedHashMap<String, LinkedList<PurchaseSearchDto>> getGroupByReceiptId(LinkedList<PurchaseNewDto> purchases) {
        LinkedHashMap<String, LinkedList<PurchaseSearchDto>> results = new LinkedHashMap<>();

        // sort
        sortDescByReceiptId(purchases);

        // Get All ReceiptID
        LinkedList<Integer> keys = new LinkedList<>();

        for (int i = 0; i < purchases.size(); i++) {
            if (!keys.contains(purchases.get(i).getReceipt_id())) {
                keys.add(purchases.get(i).getReceipt_id());
            }
        }

        LinkedList<PurchaseSearchDto> temp = null;

        for (Integer k : keys) {
            for (int i = 0; i < purchases.size(); i++) {
                temp = new LinkedList<>();

                if (purchases.get(i).getReceipt_id() == k.intValue()) {
                    PurchaseSearchDto p = new PurchaseSearchDto(purchases.get(i));
                    temp.add(p);
                }
            }

            if (temp != null && temp.size() > 0) {
                results.put(insertZeroString(k.intValue()), temp);
            }
        }

        return results;
    }
}
