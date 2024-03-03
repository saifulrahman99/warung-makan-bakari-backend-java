package com.enigma.wmb_api.service.impl;

import com.enigma.wmb_api.constant.EnumTransType;
import com.enigma.wmb_api.dto.request.BillRequest;
import com.enigma.wmb_api.dto.response.BillDetailResponse;
import com.enigma.wmb_api.dto.response.BillResponse;
import com.enigma.wmb_api.entity.*;
import com.enigma.wmb_api.repository.BillRepository;
import com.enigma.wmb_api.service.*;
import com.enigma.wmb_api.specification.BillSpecification;
import com.enigma.wmb_api.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillServiceImpl implements BillService {
    private final BillRepository billRepository;
    private final BillDetailService billDetailService;
    private final CustomerService customerService;
    private final MsTableService tableService;
    private final TransTypeService transTypeService;
    private final MenuService menuService;
    private final ValidationUtil validationUtil;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public BillResponse create(BillRequest request) {
        validationUtil.validate(request);
        // butuh customer, table dan trans type
        Customer customer = customerService.findById(request.getCustomer());
        TransType transType;

        if (request.getTable() != null) transType = transTypeService.findOrSave(EnumTransType.DI);
        else transType = transTypeService.findOrSave(EnumTransType.TA);

        MsTable table = (request.getTable() != null) ? tableService.findById(request.getTable()) : null;

        Bill trx = Bill.builder()
                .transDate(new Date())
                .customer(customer)
                .table(table)
                .transType(transType)
                .build();
        billRepository.saveAndFlush(trx);

        List<BillDetail> trxDetails = request.getBillDetails().stream()
                .map(detailRequest -> {
                    Menu menu = menuService.findById(detailRequest.getMenuId());
                    return BillDetail.builder()
                            .bill(trx)
                            .menu(menu)
                            .qty(detailRequest.getQty())
                            .price(menu.getPrice())
                            .build();
                }).toList();
        trx.setBillDetails(trxDetails);
        billDetailService.createBulk(trxDetails);

        List<BillDetailResponse> trxDetailResponse = trxDetails.stream()
                .map(billDetail -> {
                    return BillDetailResponse.builder()
                            .id(billDetail.getId())
                            .menuId(billDetail.getMenu().getId())
                            .price(billDetail.getPrice())
                            .quantity(billDetail.getQty())
                            .build();
                }).toList();

        return BillResponse.builder()
                .id(trx.getId())
                .transDate(String.valueOf(trx.getTransDate()))
                .customerId(trx.getCustomer().getId())
                .tableId((request.getTable() != null) ? trx.getTable().getId() : null)
                .transType(trx.getTransType().getId().name())
                .billDetails(trxDetailResponse)
                .build();
    }

    @Override
    public Page<BillResponse> findAll(BillRequest request) {
        if (request.getPage() <= 0) request.setPage(1);
        Sort sort = Sort.by(Sort.Direction.fromString(request.getDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of((request.getPage() - 1), request.getSize(), sort);
        Specification<Bill> specification = BillSpecification.getSpecification(request);
        Page<Bill> bills = billRepository.findAll(specification, pageable);

        List<BillResponse> billResponses = bills.getContent().stream().map(bill -> {
            // buat bill detail response
            List<BillDetailResponse> billDetailResponses = bill.getBillDetails().stream()
                    .map(detail -> BillDetailResponse.builder()
                            .id(detail.getId())
                            .menuId(detail.getMenu().getId())
                            .quantity(detail.getQty())
                            .price(detail.getPrice())
                            .build())
                    .toList();
            // log.info(String.valueOf(bill.getTransDate()));
            return BillResponse.builder()
                    .id(bill.getId())
                    .customerId(bill.getCustomer().getId())
                    .tableId((bill.getTable() != null) ? bill.getTable().getId() : null)
                    .transDate(String.valueOf(bill.getTransDate()))
                    .transType(bill.getTransType().getId().name())
                    .billDetails(billDetailResponses)
                    .build();
        }).toList();
        return new PageImpl<>(billResponses, pageable, bills.getTotalElements());
    }

    @Override
    public BillResponse findById(String id) {
        return null;
    }

}
