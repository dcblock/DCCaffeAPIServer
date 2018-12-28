package com.digicap.dcblock.caffeapiserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import jdk.nashorn.internal.objects.annotations.Property;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class SettlementReport {

  @NotNull
  private String name;

  @NotNull
  private String company;

  @NotNull
  private String email;

  @JsonProperty("total_price")
  private long totalPrice;

  @JsonProperty("total_dc_price")
  private long totalDcPrice;

  @JsonProperty("billing_amount")
  private long billingAmount;

  @JsonProperty("user_record_index")
  private long userRecordIndex;
}
