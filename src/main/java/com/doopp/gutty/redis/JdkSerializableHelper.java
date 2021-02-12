package com.doopp.gutty.redis;

import java.io.*;

public class JdkSerializableHelper implements SerializableHelper {

    @Override
    public byte[] serialize(Object obj){
        byte[] bytes = null;
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ObjectOutputStream oos=new ObjectOutputStream(baos);
            oos.writeObject(obj);
            bytes=baos.toByteArray();
            baos.close();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Object obj=null;
        try {
            ByteArrayInputStream bais=new ByteArrayInputStream(bytes);
            ObjectInputStream ois=new ObjectInputStream(bais);
            obj=ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (obj==null) ? null : clazz.cast(obj);
    }
}
