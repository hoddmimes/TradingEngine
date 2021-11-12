package com.hoddmimes.te.management.gui.table;

import javax.swing.*;

public interface ModelRenderCallback
{
    public void tableCellRendererComponent(JLabel pCellRenderObject, JTable pTable, Object pValue, int pRow, int pCol  );
}
