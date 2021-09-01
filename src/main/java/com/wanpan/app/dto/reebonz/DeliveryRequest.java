package com.wanpan.app.dto.reebonz;

import javassist.NotFoundException;
import lombok.Data;

@Data
public class DeliveryRequest {

    private long orderedItemId; //송장 입력할 주문 아이템 번호
    private int deliveryMethodId; //택배사 아이디
    private String trackingCode; //입력할 송장 번호

    public enum ReebonzDeliveryMethod{
        //Reboonz
//        CJ_GLS(1),
//        우체국택배(2),
//        우체국EMS(3),
//        FedEx(4),
//        로젠택배(5),
//        대한통운(6),
//        한진택배(7),
//        롯데택배(8),
//        DHL(9),
//        동부익스프레스(10),
//        KGB택배(11),
//        대신택배(12),
//        UPS(13),
//        KG옐로우캡택배(14),
//        경동택배(15),
//        대한통운_국제택배(16),
//        드림택배_구_KG로지스(17),
//        TNT(20),
//        USPS(21),
//        일양로지스(22),
//        GSMNTON(24),
//        DGF(25);


        HYUNDAI("롯데택배"),
        KGB	("로젠택배"),
        EPOST	("우체국"),
        HANJIN	("한진택배"),
        CJGLS("CJ대한통운"),
        KDEXP("경동택배"),
        DIRECT("업체직송"),//임의의 숫자로만 입력, 배송트래킹 지원안함
        ILYANG("일양택배"),
        CHUNIL("천일특송"),
        AJOU("아주택배"),
        CSLOGIS("SC로지스"),
        DAESIN("대신택배"),
        CVS("CVS택배"),//대한통운과 동일
        HDEXP("합동택배"),
        DHL("DHL"),
        UPS("UPS"),
        FEDEX("FEDEX"),
        REGISTPOST("우편등기"),
        EMS("우체국"),//EMS - 앞 2자리 영문, 뒤 2자리 영문(국가코드), 나머지 숫자
        TNT("TNT"),
        USPS("USPS"),
        IPARCEL("i-parcel"),
        GSMNTON("GSM NtoN"),
        SWGEXP("성원글로벌"),
        PANTOS("범한판토스"),
        ACIEXPRESS("ACI Express"),
        DAEWOON("대운글로벌"),
        AIRBOY("에어보이익스프레스"),
        KGLNET("KGL네트웍스"),
        KUNYOUNG("건영택배"),//	10
        SLX("SLX택배"),//12	200016500003
        HONAM("호남택배"),//	-
        LINEEXPRESS("LineExpress"),//	13	6063032562343
        TWOFASTEXP("2FastsExpress"),//	10,12,13
        HPL("한의사랑택배"),//	10,12
        GOODSTOLUCK("굿투럭"),//	-
        KOREXG("CJ대한통운특"),//	-
        HANDEX("한덱스"),//	-
        BGF("BGF"),//	10,12 	대한통운과 동일
        ECMS("ECMS익스프레스"),//	16	ESFSZ11000047679
        WONDERS("원더스퀵"),//	15
        YONGMA("용마로지스"),//	-
        SEBANG("세방택배"),//	13
        NHLOGIS("농협택배"),//	-
        LOTTEGLOBAL("롯데글로벌"),//	-
        GSIEXPRESS("GSI익스프레스"),//	13
        EFS("EFS"),//	18	802279800499118623
        DHLGLOBALMAIL("DHL GlobalMail"),//	-
        GPSLOGIX("GPS로직"),//	-
        CRLX("시알로지텍"),//	12,13
        BRIDGE("브리지로지스"),//	-
        HOMEINNOV("홈이노베이션로지스"),//	12
        CWAY("씨웨이"),//	12,13
        GNETWORK("자이언트"),//	15
        ACEEXP("ACE Express"),//	13
        WEVILL("우리동네택배"),//	12
        FOREVERPS("퍼레버택배"),//	11
        WARPEX("워펙스"),//	12
        QXPRESS("큐익스프레스	"),//10,12
        SMARTLOGIS("스마트로지스"),//	12,13
        LGE("LG전자"),//	-
        WINION("위니온"),//	-
        WINION2("위니온(에어컨)");

        private final String code;

        ReebonzDeliveryMethod(String code) {
            this.code = code;
        }

        public static ReebonzDeliveryMethod fromName(String name) throws Exception {
            try {
                return ReebonzDeliveryMethod.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new NotFoundException("DeliveryMethodTypeError");
            }
        }

        public String getCode() {
            return code;
        }
    }
}
