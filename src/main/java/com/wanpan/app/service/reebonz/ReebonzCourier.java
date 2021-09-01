package com.wanpan.app.service.reebonz;

public enum ReebonzCourier {
    CJGLS("CJ GLS", "1"),
    EPOST("우체국택배", "2"),
    EMS("우체국EMS", "3"),
    FEDEX("FedEx", "4"),
    LOGEN("로젠택배", "5"),
    CJDAEHAN("대한통운", "6"),
    HANJIN("한진택배", "7"),
    HYUNDAI("롯데택배", "8"),
    DHL("DHL", "9"),
    DONGBU("동부익스프레스", "10"),
    KGB("KGB택배", "11"),
    DAESIN("대신택배", "12"),
    UPS("UPS", "13"),
    YELLOWCAP("KG옐로우캡택배", "14"),
    KDEXP("경동택배", "15"),
    KOREXG("대한통운(국제택배)", "16"),
    KGLOGIS("드림택배(구 KG로지스)", "17"),
    DIRECT("직접배송", "18"),
    QUICK("퀵배송", "19"),
    TNT("TNT", "20"),
    USPS("USPS", "21"),
    ILYANG("일양로지스", "22"),
    DREAM("드림택배", "23"),
    GSMNTON("GSMNTON", "24"),
    DGF("DGF", "25"),
    ACIEXPRESS("ACI", "26"),
    DAERIM("대림통운", "27"),
    LOTTEG("롯데택배 해외특송", "28");


    private final String name;
    private final String code;

    ReebonzCourier(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return this.name;
    }
    public String getCode() {
        return this.code;
    }

    public static ReebonzCourier fromName(String name) {
        for (ReebonzCourier reebonzCourier : ReebonzCourier.values()) {
            if (reebonzCourier.getName().equals(name)) {
                return reebonzCourier;
            }
        }
        return null;
    }

}
