package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.others.CurrencyManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/14/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class AmountAdapter {
    private static final String TAG = "AmountAdapter";
    public static boolean comaHasBeenInserted = false;
    private static final int DIGITS_LIMIT = 12;
    private static boolean isTextColorGrey = true;
    private static String rightValue = "0";
    private static String leftValue = "0";
    private static final int MAX_DIGITS_AFTER_SEPARATOR = 2;
    public static int digitsInserted = 0;

    public static void preConditions(String tmp) {
        switch (tmp) {
            case "":
                doBackSpace();
                break;
            case ".":
                insertSeparator();
                break;
            default:
                insertDigit(tmp);
                break;
        }
    }

    public static void doBackSpace() {
        String amount = rightValue;
        Log.d(TAG, "digitsInserted: " + digitsInserted);
        int length = amount.length();
        if (comaHasBeenInserted) {
            if (digitsInserted > 0) {
                digitsInserted--;
            } else {
                comaHasBeenInserted = false;
                CurrencyManager.separatorNeedsToBeShown = false;
            }
            if (rightValue.equals("0.")) {
                changeTextColor(2);
                calculateAndPassValuesToFragment(rightValue.substring(0, length - 1));
            }
        }
        if (length > 1) {
            if (rightValue.length() == 3 && rightValue.substring(0, 2) == "0.") {
                calculateAndPassValuesToFragment(rightValue.substring(0, length - 2));
            } else {
                calculateAndPassValuesToFragment(rightValue.substring(0, length - 1));
            }
        } else {
            changeTextColor(2);
            calculateAndPassValuesToFragment("0");
        }

    }

    public static void insertSeparator() {
        if (isTextColorGrey) {
            changeTextColor(1);
        }
        String amount = rightValue;
        CurrencyManager.separatorNeedsToBeShown = true;
        if (!comaHasBeenInserted) {
            comaHasBeenInserted = true;
            calculateAndPassValuesToFragment(amount + ".");
        }
    }

    public static void insertDigit(String tmp) {
        String amount = rightValue;
//        Log.e(TAG, "The text before inserting digits: " + amount);
        int length = amount.length();

        if (isTextColorGrey) {
            changeTextColor(1);
        }
        if (isDigitInsertingLegal(amount)) {

            if (comaHasBeenInserted) {
                digitsInserted++;
            }
            if (length == 1 && amount.equals("0")) {
                calculateAndPassValuesToFragment(tmp);
            } else {
                calculateAndPassValuesToFragment(rightValue + tmp);
            }
        }

//        Log.e(TAG, "The text after inserting digits: " + rightValue);

    }

    private static boolean isDigitInsertingLegal(String text) {
//        Log.e(TAG, "Testing isDigitInsertingLegal, text: " + text);
        if (text.length() < DIGITS_LIMIT) {
            if (comaHasBeenInserted) {
                return digitsInserted < MAX_DIGITS_AFTER_SEPARATOR;
            }
            return true;
        }
        return false;
    }

    /**
     * Sets the textColor of the amount TextView to black or grey
     *
     * @patam color the color of the textView: 1 Black, 2 Grey.
     */
    private static void changeTextColor(int color) {
        Activity context = MainActivity.app;
        isTextColorGrey = color == 1 ? false : true;
        FragmentScanResult.amountToPay.setTextColor((color == 1) ? context.getResources().getColor(R.color.black)
                : context.getResources().getColor(android.R.color.darker_gray));
    }

    public static void resetKeyboard() {
        comaHasBeenInserted = false;
        isTextColorGrey = true;
        rightValue = "0";
        leftValue = "0";
        CurrencyManager.separatorNeedsToBeShown = false;
        FragmentScanResult.currentCurrencyPosition = FragmentScanResult.BITCOIN_RIGHT;
        digitsInserted = 0;
        calculateAndPassValuesToFragment("0");
    }

    public static void calculateAndPassValuesToFragment(String valuePassed) {
        Log.d(TAG, "This is the value passed: " + valuePassed);
        rightValue = valuePassed;
        try {
            BigDecimal rightValueObject = new BigDecimal(valuePassed);
            BigDecimal leftValueObject;
            BigDecimal rate = new BigDecimal(FragmentScanResult.rate);
            if (rightValueObject.equals(new BigDecimal("0"))) {
                leftValueObject = new BigDecimal("0");
            } else {
                if (FragmentScanResult.currentCurrencyPosition == FragmentScanResult.BITCOIN_RIGHT) {
                    //from bits to other currency using rate
                    leftValueObject = rate.multiply(rightValueObject.divide(new BigDecimal("1000000")));
                } else if (FragmentScanResult.currentCurrencyPosition == FragmentScanResult.BITCOIN_LEFT) {
                    //from other currency to bits using rate
                    leftValueObject = rightValueObject.multiply(new BigDecimal("1000000")).divide(rate, RoundingMode.CEILING);
                } else {
                    throw new IllegalArgumentException("currentCurrencyPosition should be BITCOIN_LEFT or BITCOIN_RIGHT");
                }
            }
            leftValue = new DecimalFormat("0.##").format(leftValueObject.doubleValue());
            FragmentScanResult.updateBothTextValues(rightValueObject, leftValueObject);
        } catch (Exception e){
            e.printStackTrace();
            FragmentScanResult.updateBothTextValues(new BigDecimal("0"), new BigDecimal("0"));
        }

    }

    public static void switchCurrencies() {
        FragmentScanResult.currentCurrencyPosition = FragmentScanResult.currentCurrencyPosition == 1 ? 2 : 1;
        String tmp = rightValue;
        rightValue = leftValue;
        leftValue = tmp;
//        Log.d(TAG, "rightValue: " + rightValue + "  leftValue: " + leftValue);
        if (rightValue.contains(".")) {
            Log.d(TAG, "Contains!: " + rightValue);
            digitsInserted = rightValue.length() - rightValue.indexOf(".") - 1;
//            Log.d(TAG, "Testing digitsInserted: " + digitsInserted);
            comaHasBeenInserted = true;
            CurrencyManager.separatorNeedsToBeShown = true;
        } else {
            Log.d(TAG, "Does not contain!");
            comaHasBeenInserted = false;
            digitsInserted = 0;
            CurrencyManager.separatorNeedsToBeShown = false;
        }

        FragmentScanResult.updateBothTextValues(new BigDecimal(rightValue), new BigDecimal(leftValue));
    }

    public static String getRightValue() {
        return rightValue;
    }

}