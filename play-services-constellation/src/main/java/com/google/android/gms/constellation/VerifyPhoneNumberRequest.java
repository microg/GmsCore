package com.google.android.gms.constellation;

import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class VerifyPhoneNumberRequest extends AutoSafeParcelable {
    @Field(1)
    public String upiPolicyId;
    @Field(2)
    public long b;
    @Field(3)
    public IdTokenRequest idTokenRequest; // IdTokenRequest{certHash='CYChK+mTUowZEHvCGtgRR4xjzvw=', tokenNonce='4b20882c8ebc75555ce0b48c1f741f040f49a0fe2e428d2caf84f10aba2184bc'},

    /**
     * strings i've seen:
     *
     * consented: true/false
     * consent_type=RCS_DEFAULT_ON_LEGAL_FYI_IN_SETTINGS
     * is_pnv_consent: true
     * api_version: integer (1, 2)
     * policy_id: string
     * one_time_verification=True
     * required_consumer_consent=RCS
     * session_id=UUID
     */
    @Field(4)
    public Bundle settings;
    @Field(5)
    public List<ImsiRequest> imsis;
    @Field(6)
    public boolean f;
    @Field(7)
    public int g;
    @Field(8)
    public List phoneNumberSelections;

    @Override
    public String toString() {
        return "VerifyPhoneNumberRequest{" +
                "upiPolicyId='" + upiPolicyId + '\'' +
                ", b=" + b +
                ", idTokenRequest=" + idTokenRequest +
                ", settings=" + bundleToString(settings) +
                ", imsis=" + imsis +
                ", f=" + f +
                ", g=" + g +
                ", phoneNumberSelections=" + phoneNumberSelections +
                '}';
    }

    public static String bundleToString(Bundle b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder("Bundle{");
        for (String key : b.keySet()) {
            sb.append(key).append("=").append(b.get(key)).append(", ");
        }
        if (sb.length() > 7) sb.setLength(sb.length()-2);
        sb.append("}");
        return sb.toString();
    }

    public static final Creator<VerifyPhoneNumberRequest> CREATOR = findCreator(VerifyPhoneNumberRequest.class);
}
