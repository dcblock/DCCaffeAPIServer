package com.digicap.dcblock.caffeapiserver.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class MenuVo {

    int category;

    int code;

    int order;

    String name_en;

    String name_kr;

    int price;

    int dc_digicap;

    int dc_covision;

    int opt_size;

    int opt_type;
}
