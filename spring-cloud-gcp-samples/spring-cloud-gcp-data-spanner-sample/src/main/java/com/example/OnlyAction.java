package com.example;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "onlyaction", types = {Trade.class})
public interface OnlyAction {

  String getAction();

}
