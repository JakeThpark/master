package com.wanpan.app.service.mustit.constant;

public enum MustitCourier {
    //DB-shop_courier_code(DB-shop_courier_name(OptionName), OptionValue(FormDataValue))
    POST_OFFICE("우체국택배", "우체국택배"),
    CJ_LOGISTICS("CJ대한통운", "CJ대한통운"),
    HANJIN("한진택배", "한진택배"),
    LOGEN("로젠택배", "로젠택배"),
    LOTTE_HYUNDAI("롯데택배<구 현대택배>", "롯데택배<구 현대택배>"),
    ILYANG_LOGIS("일양로지스", "일양로지스"),
    EMS("EMS", "EMS"),
    DHL("DHL", "DHL"),
    UPS("UPS택배", "UPS택배"),
    HPL("한의사랑택배", "한의사랑택배"),
    CHUNIL("천일택배", "천일택배"),
    KUNYOUNG("건영택배", "건영택배"),
    KORYO_LOGISTICS("고려택배", "고려택배"),
    HANDEX("한덱스", "한덱스"),
    FEDEX("Fedex", "Fedex"),
    DAESIN("대신택배", "대신택배"),
    KYOUNGDONG("경동택배", "경동택배"),
    CVSNET_POSTBOX("CVSnet 편의점택배", "CVSnet 편의점택배"),
    TNT_EXPRESS("TNT Express", "TNT Express"),
    USPS("USPS택배", "USPS택배"),
    TPL("TPL", "TPL"),
    GSMN_TO_N("GSMNtoN(인로스)", "GSMNtoN(인로스)"),
    AIR_BOY_EXPRESS("에어보이익스프레스", "에어보이익스프레스"),
    KGL_NETWORKS("KGL네트웍스", "KGL네트웍스"),
    HAPDONG("합동택배", "합동택배"),
    DHL_GLOBAL_MAIL("DHL Global Mail", "DHL Global Mail"),
    I_PARCEL("i-Parcel", "i-Parcel"),
    PANTOS("범한판토스", "범한판토스"),
    APEX_ECMS_EXPRESS("APEX(ECMS Express)", "APEX(ECMS Express)"),
    GOODS_TO_LUCK("굿투럭", "굿투럭"),
    GSI_Express("GSI Express", "GSI Express"),
    CJ_LOGISTICS_INTERNATIONAL("CJ대한통운 국제특송", "CJ대한통운 국제특송"),
    ANYTRACK("애니트랙", "애니트랙"),
    SLX("SLX", "SLX"),
    HONAM_LOGIS("호남택배", "호남택배"),
    CU_POST("CU편의점택배", "CU편의점택배"),
    WOORI_HANBANG("우리한방택배", "우리한방택배"),
    ACI_EXPRESS("ACIExpress", "ACIExpress"),
    ACE_EXPRESS("ACE Express", "ACE Express"),
    SUNGWON_GLOBAL("성원글로벌", "성원글로벌"),
    SEBANG("세방", "세방"),
    NONGHYUP_LOGIS("농협택배", "농협택배"),
    HOMEPICK("홈픽택배", "홈픽택배"),
    EUROPARCEL("EuroParcel(유로택배)", "EuroParcel(유로택배)"),
    KGB("KGB택배", "KGB택배"),
    CWAY_EXPRESS("Cway Express", "Cway Express"),
    HYBRID_LOGIS("하이택배", "하이택배"),
    YJS_GLOBAL_UK("YJS글로벌(영국)", "YJS글로벌(영국)"),
    WARP_EX("워팩스코리아", "워팩스코리아"),
    HOMEINNOVATION_LOGIS("홈이노베이션로지스", "홈이노베이션로지스"),
    EUNHA_SHIPPING("은하쉬핑", "은하쉬핑"),
    FLF_FOREVER("FLF퍼레버택배", "FLF퍼레버택배"),
    YJS_GLOBAL_WORLD("YJS글로벌(월드)", "YJS글로벌(월드)"),
    GIANT_EXPRESS("Giant Express", "Giant Express"),
    DD_LOGIS("디디로지스", "디디로지스"),
    DAELIM("대림통운", "대림통운"),
    LOTOS_CORPORATION("LOTOS CORPORATION", "LOTOS CORPORATION"),
    IK82("IK물류", "IK물류"),
    SUNGHUN("성훈물류", "성훈물류"),
    CR_LOGITECH("CR로지텍", "CR로지텍"),
    YONGMA_LOGIS("용마로지스", "용마로지스"),
    WONDERS("원더스퀵", "원더스퀵"),
    DAEWOON_GLOBAL("대운글로벌", "대운글로벌"),
    LINE_EXPRESS("LineExpress", "LineExpress"),
    TWOFAST_EXPRESS("2FastExpress", "2FastExpress"),
    TPM_KOREA_YONGDAL("티피엠코리아(주) 용달이 특송", "티피엠코리아(주) 용달이 특송"),
    L_SERVICE_LOGIS("엘서비스", "엘서비스"),
    ZENIEL_SYSTEM("제니엘시스템", "제니엘시스템"),
    FRESH_SOLUTIONS("프레시솔루션", "프레시솔루션"),
    J_LOGIST("제이로지스트", "제이로지스트"),
    SMART_LOGIS("스마트로지스", "스마트로지스"),
    FULL_AT_HOME("풀앳홈", "풀앳홈"),
    ETOMARS("이투마스(ETOMARS)", "이투마스(ETOMARS)"),
    LOTTE_GLOBAL_LOGIS("롯데글로벌 로지스", "롯데글로벌 로지스"),
    QUICKQUICK_DOT_COM("퀵퀵닷컴", "퀵퀵닷컴"),
    CVSNET_POSTBOX_QUICK("CVSnet postbox퀵", "CVSnet postbox퀵"),
    SKYNET_WORLDWIDE_EXPRESS("SkyNet Worldwide Express", "SkyNet Worldwide Express"),
    WIZWA("WIZWA", "WIZWA"),
    SUNGWON_GLOBAL_CARGO("성원글로벌카고", "성원글로벌카고"),
    AL_EXPRESS("Alexpress", "Alexpress"),
    QUICK_SERVICE("퀵배송", "퀵배송"),
    SURPRISE_SERVICE("깜짝배송", "깜짝배송"),
    CUSTOM_COURIER_NAME("직접입력", "직접입력"),
    DREAM_KBLOGICS("드림택배<구 KG로지스>", "드림택배<구 KG로지스>");

    private final String name;
    private final String code;

    MustitCourier(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public static MustitCourier getByName(String name) {
        for (MustitCourier mustitCourier : MustitCourier.values()) {
            if (mustitCourier.getName().equals(name)) {
                return mustitCourier;
            }
        }
        return null;
    }
}
