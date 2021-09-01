package com.wanpan.app.service.mustit.constant;

import com.wanpan.app.dto.job.order.OrderJobDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MustitOrderStatus {

    PAYMENT_COMPLETE("발송요청",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.DELIVERY_READY,
                    OrderJobDto.Request.OrderUpdateActionStatus.DELIVERY,
                    OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL
            )
    ),
    PAYMENT_COMPLETE_BY_EXCHANGE("발송요청(교환)",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.EXCHANGE_CONFIRM,
                    OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL
            )
    ),
    DELIVERY_READY("배송준비중",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.DELIVERY,
                    OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL
            )
    ),
    DELIVERY_READY_BY_EXCHANGE("배송준비중(교환)",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.EXCHANGE_CONFIRM,
                    OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL
            )
    ),
    DELIVERY("배송중",
            Collections.emptyList()
    ),
    DELIVERY_COMPLETE("배송완료",
            Collections.emptyList()
    ),
    BUY_CANCEL_REQUEST("구매취소요청",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.BUY_CANCEL_CONFIRM,
                    OrderJobDto.Request.OrderUpdateActionStatus.BUY_CANCEL_REJECT
            )
    ),
    DELIVERY_BY_EXCHANGE("배송중(교환)",
            Collections.emptyList()
    ),
    EXCHANGE_REQUEST("교환요청",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.EXCHANGE_CONFIRM,
                    OrderJobDto.Request.OrderUpdateActionStatus.EXCHANGE_REJECT
            )
    ),
    RETURN_REQUEST("반품요청",
            Arrays.asList(
                    OrderJobDto.Request.OrderUpdateActionStatus.RETURN_CONFIRM,
                    OrderJobDto.Request.OrderUpdateActionStatus.RETURN_REJECT
            )
    ),
    RETURN_CONFIRM("반품성사",
            Collections.emptyList()
    ),
    SELL_CANCEL("판매취소",
            Collections.emptyList()
    ),
    CALCULATION_SCHEDULE("정산예정",
            Collections.singletonList(OrderJobDto.Request.OrderUpdateActionStatus.CALCULATION_DELAY)
    ),
    CALCULATION_DELAY("정산보류중",
            Collections.singletonList(OrderJobDto.Request.OrderUpdateActionStatus.CALCULATION_SCHEDULE)
    ),
    CALCULATION_COMPLETE("정산완료",
            Collections.emptyList()
    ),
    BUY_CANCEL_COMPLETE("구매취소완료",
            Collections.emptyList()
    ),
    RETURN_COMPLETE("반품환불완료",
            Collections.emptyList()
    ),
    SELL_CANCEL_COMPLETE("판매취소완료",
            Collections.emptyList()
    );

    private final String code;
    private final List<OrderJobDto.Request.OrderUpdateActionStatus> availableOrderUpdateActionStatusList;

    MustitOrderStatus(String code, List<OrderJobDto.Request.OrderUpdateActionStatus> availableOrderUpdateActionStatusList) {
        this.code = code;
        this.availableOrderUpdateActionStatusList = availableOrderUpdateActionStatusList;
    }

    public String getCode() {
        return this.code;
    }

    public List<OrderJobDto.Request.OrderUpdateActionStatus> getAvailableOrderUpdateActionStatusList() {
        return this.availableOrderUpdateActionStatusList;
    }

    public boolean isUpdatableTo(OrderJobDto.Request.OrderUpdateActionStatus orderUpdateActionStatus) {
        return this.getAvailableOrderUpdateActionStatusList().contains(orderUpdateActionStatus);
    }

    public static MustitOrderStatus getByCode(String code) {
        for (MustitOrderStatus mustitOrderStatus : MustitOrderStatus.values()) {
            if (mustitOrderStatus.getCode().equals(code)) {
                return mustitOrderStatus;
            }
        }

        return null;
    }

    public static List<MustitOrderStatus> getTargetStatusListByUpdateAction(MustitOrderStatus currentStatus, OrderJobDto.Request.OrderUpdateActionStatus updateAction) {
        List<MustitOrderStatus> targetStatusList = new ArrayList<>();
        switch (currentStatus) {
            case PAYMENT_COMPLETE:
                switch (updateAction) {
                    case DELIVERY_READY:
                        targetStatusList.add(MustitOrderStatus.DELIVERY_READY);
                        break;
                    case DELIVERY:
                        targetStatusList.add(MustitOrderStatus.DELIVERY);
                        break;
                    case SELL_CANCEL:
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL);
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL_COMPLETE);
                        break;
                    default:
                        break;
                }
                break;
            case DELIVERY_READY:
                switch (updateAction) {
                    case DELIVERY:
                        targetStatusList.add(MustitOrderStatus.DELIVERY);
                        break;
                    case SELL_CANCEL:
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL);
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL_COMPLETE);
                        break;
                    default:
                        if (OrderJobDto.Request.OrderUpdateActionStatus.DELIVERY_READY.equals(updateAction)) {
                            // 머스트잇 주문상태가 이미 "배송준비중"인데, "배송준비중"으로 업데이트 하는 경우
                            targetStatusList.add(MustitOrderStatus.DELIVERY_READY);
                        }
                        break;
                }
                break;
            case DELIVERY:
                if (Arrays.asList(
                        OrderJobDto.Request.OrderUpdateActionStatus.DELIVERY,
                        OrderJobDto.Request.OrderUpdateActionStatus.EXCHANGE_REJECT,
                        OrderJobDto.Request.OrderUpdateActionStatus.RETURN_REJECT,
                        OrderJobDto.Request.OrderUpdateActionStatus.BUY_CANCEL_REJECT).contains(updateAction)) {
                    // 머스트잇 주문상태가 이미 "배송중"인데, 배송중/교환거절/반품거절/구매취소거절 하는 경우
                    targetStatusList.add(MustitOrderStatus.DELIVERY);
                }
                break;
            case DELIVERY_BY_EXCHANGE:
                if (Arrays.asList(
                        OrderJobDto.Request.OrderUpdateActionStatus.EXCHANGE_CONFIRM,
                        OrderJobDto.Request.OrderUpdateActionStatus.BUY_CANCEL_REJECT).contains(updateAction)) {
                    // 머스트잇 주문상태가 이미 "배송중(교환)"인데, 교환승인/구매취소거절 하는 경우
                    targetStatusList.add(MustitOrderStatus.DELIVERY_BY_EXCHANGE);
                }
                break;
            case PAYMENT_COMPLETE_BY_EXCHANGE:
            case DELIVERY_READY_BY_EXCHANGE:
                switch (updateAction) {
                    case EXCHANGE_CONFIRM:
                        targetStatusList.add(MustitOrderStatus.DELIVERY_BY_EXCHANGE);
                        break;
                    case SELL_CANCEL:
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL);
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL_COMPLETE);
                        break;
                    default:
                        break;
                }
                break;
            case EXCHANGE_REQUEST:
                switch (updateAction) {
                    case EXCHANGE_CONFIRM:
                        targetStatusList.add(MustitOrderStatus.DELIVERY_BY_EXCHANGE);
                        break;
                    case EXCHANGE_REJECT:
                        targetStatusList.add(MustitOrderStatus.DELIVERY);
                        break;
                    case SELL_CANCEL:
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL);
                        targetStatusList.add(MustitOrderStatus.SELL_CANCEL_COMPLETE);
                        break;
                    default:
                        break;
                }
                break;
            case RETURN_REQUEST:
                switch (updateAction) {
                    case RETURN_CONFIRM:
                        targetStatusList.add(MustitOrderStatus.RETURN_CONFIRM);
                        targetStatusList.add(MustitOrderStatus.RETURN_COMPLETE);
                        break;
                    case RETURN_REJECT:
                        targetStatusList.add(MustitOrderStatus.DELIVERY);
                        break;
                    default:
                        break;
                }
                break;
            case RETURN_CONFIRM:
            case RETURN_COMPLETE:
                if (OrderJobDto.Request.OrderUpdateActionStatus.RETURN_CONFIRM.equals(updateAction)) {
                    // 머스트잇 주문상태가 이미 "반품성사"/"반품환불완료"인데, 반품승인하는 경우
                    targetStatusList.add(MustitOrderStatus.RETURN_CONFIRM);
                    targetStatusList.add(MustitOrderStatus.RETURN_COMPLETE);
                }
                break;
            case BUY_CANCEL_REQUEST:
                switch (updateAction) {
                    case BUY_CANCEL_CONFIRM:
                        targetStatusList.add(MustitOrderStatus.BUY_CANCEL_COMPLETE);
                        break;
                    case BUY_CANCEL_REJECT:
                        targetStatusList.add(MustitOrderStatus.DELIVERY);
                        targetStatusList.add(MustitOrderStatus.DELIVERY_BY_EXCHANGE);
                        break;
                    default:
                        break;
                }
                break;
            case BUY_CANCEL_COMPLETE:
                if (OrderJobDto.Request.OrderUpdateActionStatus.BUY_CANCEL_CONFIRM.equals(updateAction)) {
                    // 머스트잇 주문상태가 이미 "구매취소완료"인데, 구매취소승인 하는 경우
                    targetStatusList.add(MustitOrderStatus.BUY_CANCEL_COMPLETE);
                }
                break;
            case SELL_CANCEL:
            case SELL_CANCEL_COMPLETE:
                if (OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL.equals(updateAction)) {
                    // 머스트잇 주문상태가 이미 "판매취소"/"판매취소완료"인데, 판매취소하는 경우
                    targetStatusList.add(MustitOrderStatus.SELL_CANCEL);
                    targetStatusList.add(MustitOrderStatus.SELL_CANCEL_COMPLETE);
                }
                break;
            case CALCULATION_SCHEDULE:
                if (OrderJobDto.Request.OrderUpdateActionStatus.CALCULATION_DELAY.equals(updateAction)) {
                    targetStatusList.add(MustitOrderStatus.CALCULATION_DELAY);
                } else if (OrderJobDto.Request.OrderUpdateActionStatus.CALCULATION_SCHEDULE.equals(updateAction)) {
                    // 머스트잇 주문상태가 이미 "정산예정"인데, "정산예정"으로 업데이트하는 경우
                    targetStatusList.add(MustitOrderStatus.CALCULATION_SCHEDULE);
                }
                break;
            case CALCULATION_DELAY:
                if (OrderJobDto.Request.OrderUpdateActionStatus.CALCULATION_SCHEDULE.equals(updateAction)) {
                    targetStatusList.add(MustitOrderStatus.CALCULATION_SCHEDULE);
                } else if (OrderJobDto.Request.OrderUpdateActionStatus.CALCULATION_DELAY.equals(updateAction)) {
                    // 머스트잇 주문상태가 이미 "정산보류중"인데, "정산보류중"으로 업데이트하는 경우
                    targetStatusList.add(MustitOrderStatus.CALCULATION_DELAY);
                }
                break;
            default:
                break;
        }

        return targetStatusList;
    }

    public static String getCodesByStatusList(List<MustitOrderStatus> mustitOrderStatusList) {
        StringBuilder targetStatus = new StringBuilder();
        for (MustitOrderStatus mustitOrderStatus : mustitOrderStatusList) {
            if (targetStatus.length() <= 0) {
                targetStatus.append(mustitOrderStatus.getCode());
            } else {
                targetStatus.append("/").append(mustitOrderStatus.getCode());
            }
        }

        return targetStatus.toString();
    }

}
