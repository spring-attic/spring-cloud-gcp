package com.example;

import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKeyColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

@Table(name = "subtrades")
public class SubTrade {
  @PrimaryKeyColumn(keyOrder = 2)
  String action;

  @PrimaryKeyColumn(keyOrder = 1)
  String symbol;

  @PrimaryKeyColumn(keyOrder = 3)
  int index;
}
