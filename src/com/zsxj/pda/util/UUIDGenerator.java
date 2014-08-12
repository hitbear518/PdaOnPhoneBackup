package com.zsxj.pda.util;

import java.util.Date;
import java.util.UUID;

/**
 * This class to generate UUID.
 * @author hill
 *
 */
public class UUIDGenerator
{
    /**
     * Generate UUID base on UUID version 3.
     * Use base string as: UUID.randomUUID() + new Date().getTime() + UUID.randomUUID()
     * @return
     */
    public UUID generateVer3UUID()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(UUID.randomUUID().toString());
        sb.append(new Date().getTime());
        sb.append(UUID.randomUUID().toString());
        
        return UUID.nameUUIDFromBytes(sb.toString().getBytes());
    }
}
