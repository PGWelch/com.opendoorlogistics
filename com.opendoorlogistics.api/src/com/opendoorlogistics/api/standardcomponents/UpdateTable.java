package com.opendoorlogistics.api.standardcomponents;

import java.io.Serializable;

import com.opendoorlogistics.api.components.ODLComponent;

public interface UpdateTable extends ODLComponent{
	Serializable createConfig(boolean isDelete);
}
