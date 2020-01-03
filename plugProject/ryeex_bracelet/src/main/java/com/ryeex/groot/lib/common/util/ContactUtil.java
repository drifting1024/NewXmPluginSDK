package com.ryeex.groot.lib.common.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

public class ContactUtil {
    public static String getContactByNumber(Context context, String number) {
        String finalContact = "";
        try {
            String[] cols = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, cols, null, null, null);
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                int numberFieldColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String itemName = cursor.getString(nameFieldColumnIndex);
                String itemNumber = cursor.getString(numberFieldColumnIndex);
                itemNumber = itemNumber.replaceAll("\\s*", "");
                if (itemNumber.equalsIgnoreCase(number)) {
                    finalContact = itemName;
                    break;
                }
            }
        } catch (Exception e) {
        }

        return finalContact;
    }
}
