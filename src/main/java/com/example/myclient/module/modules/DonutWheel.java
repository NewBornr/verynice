package com.example.myclient.module.modules;

import com.example.myclient.gui.DonutWheelScreen;
import com.example.myclient.module.Category;
import com.example.myclient.module.Module;
import com.example.myclient.util.Mc;

public class DonutWheel extends Module {
    public DonutWheel() {
        super("Donut Wheel", "Opens a wheel for Donut SMP commands and actions", Category.DONUT);
    }

    @Override
    protected void onEnable() {
        Mc.setScreen(new DonutWheelScreen());
        setEnabled(false); // acts like a button, not a persistent toggle
    }
}
