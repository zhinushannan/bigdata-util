package io.github.zhinushannan.util.hbase;

import io.github.zhinushannan.constant.ConvertConst;
import io.github.zhinushannan.util.common.FieldClassUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 和HBase相关的工具类，包括：
 * - 配置对象、连接对象、admin对象的获取与关闭
 * - 命名空间的删除
 * - 表的删除
 * - 扫描结果转实体类对象
 * - 实体类对象转put对象
 *
 * @author zhinushannan
 */
public class HBaseUtils {

    /**
     * hbase配置对象
     */
    private static Configuration conf = null;
    /**
     * hbase连接对象
     */
    private static Connection connection = null;
    /**
     * hbase admin 对象
     */
    private static Admin admin = null;

    /**
     * 获取hbase配置对象
     */
    public static Configuration getConf() {
        if (null == conf) {
            conf = HBaseConfiguration.create();
        }
        return conf;
    }

    /**
     * 获取hbase连接
     */
    public static Connection getConnection() throws IOException {
        if (null == connection) {
            connection = ConnectionFactory.createConnection(getConf());
        }
        return connection;
    }

    /**
     * 获取admin对象
     */
    public static Admin getAdmin() throws IOException {
        if (null == admin) {
            admin = getConnection().getAdmin();
        }
        return admin;
    }

    /**
     * 如果命名空间存在，则删除
     */
    public static void deleteNamespaceIfExist(String namespace) throws IOException {
        List<String> namespaces = Arrays.stream(getAdmin().listNamespaces()).collect(Collectors.toList());
        if (namespaces.contains(namespace)) {
            TableName[] tableNames = getAdmin().listTableNamesByNamespace(namespace);
            for (TableName tableName : tableNames) {
                deleteTableIfExist(tableName);
            }
            getAdmin().deleteNamespace(namespace);
        }
    }

    /**
     * 如果表存在，则删除
     */
    public static void deleteTableIfExist(TableName tableName) throws IOException {
        if (getAdmin().tableExists(tableName)) {
            getAdmin().disableTable(tableName);
            getAdmin().deleteTable(tableName);
        }
    }

    /**
     * 根据扫描结果Cell得到对象
     *
     * @param cls    实体类类型
     * @param result 扫描结果Cell
     * @param family 列簇
     * @return 返回扫描结果Cell对应的对象
     */
    public static <T> T getInstance(Result result, byte[] family, Class<T> cls) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return getInstance(result, family, "", cls);
    }

    /**
     * 根据扫描结果Cell得到对象
     *
     * @param cls       实体类类型
     * @param result    扫描结果Cell
     * @param family    列簇
     * @param colPrefix 列名的前缀
     * @return 返回扫描结果Cell对应的对象
     */
    public static <T> T getInstance(Result result, byte[] family, String colPrefix, Class<T> cls) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Class<?>> privateFieldClass = FieldClassUtils.getPrivateFieldClass(cls);

        T instance = cls.newInstance();

        Set<Map.Entry<String, Class<?>>> entries = privateFieldClass.entrySet();
        for (Map.Entry<String, Class<?>> entry : entries) {
            String fieldName = entry.getKey();
            Class<?> fieldCls = privateFieldClass.get(fieldName);
            Object fieldValue = HBaseUtils.getColVal(result, family, Bytes.toBytes(colPrefix + fieldName), fieldCls);
            getSetMethod(fieldName, fieldCls, cls).invoke(instance, fieldValue);
        }

        return instance;
    }

    /**
     * 根据扫描结果集得到对象列表
     *
     * @param cls     实体类类型
     * @param results 扫描结果集
     * @param family  列簇
     * @return 返回扫描结果Cell对应的对象
     */
    public static <T> List<T> getInstances(ResultScanner results, byte[] family, Class<T> cls) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        return getInstances(results, family, "", cls);
    }

    public static <T> List<T> getInstances(ResultScanner results, byte[] family, String colPrefix, Class<T> cls) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        List<T> list = new ArrayList<>();

        for (Result result : results) {
            list.add(getInstance(result, family, colPrefix, cls));
        }

        return list;
    }


    /**
     * 打印扫描结果
     *
     * @param cls     扫描结果的值的类型
     * @param results 扫描结果集
     * @param family  列簇
     */
    public static <T> void show(ResultScanner results, byte[] family, Class<T> cls) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        StringBuffer stringBuffer;

        for (Result result : results) {
            stringBuffer = new StringBuffer();

            Object rowKey = getBytesToMethod(String.class).invoke(null, result.getRow());

            stringBuffer.append("{").append(rowKey).append("----");

            T instance = getInstance(result, family, cls);
            stringBuffer.append(instance);

            stringBuffer.append("}");
            System.out.println(stringBuffer);
        }
    }

    /**
     * 根据数据类型获取对应的转换方法，如传入 int.class ，返回的是 Bytes 类中的 toInt(byte[]) 方法
     * 由于该方法不需要对外暴露，下个版本方法将设为private
     *
     * @param cls 扫描结果的值的类型
     */
    @Deprecated
    public static Method getBytesToMethod(Class<?> cls) throws NoSuchMethodException {
        cls = ConvertConst.wrapper2primitive.get(cls) == null ? cls : ConvertConst.wrapper2primitive.get(cls);
        String name = cls.getSimpleName();
        Class<Bytes> bytesClass = Bytes.class;
        return bytesClass.getMethod("to" + name.substring(0, 1).toUpperCase() + name.substring(1), byte[].class);
    }

    /**
     * 根据数据类型、结果集和列簇中的列名获取对应的值。
     *
     * @param cls    转换为byte[]之前的数据类型
     * @param result hbase扫描结果
     * @param family 列簇
     * @param col    列
     * @return 返回结果
     */
    public static Object getColVal(Result result, byte[] family, byte[] col, Class<?> cls) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return getBytesToMethod(cls).invoke(null, result.getValue(family, col));
    }


    /**
     * 根据实体类对象和列簇构建put对象
     *
     * @param family 列簇
     * @param rowKey 行键
     * @param t      实体类对象
     * @param <T>    实体类对象的类型
     * @return 返回 Put 对象
     */
    public static <T> Put getPut(byte[] family, byte[] rowKey, T t) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return getPut(rowKey, family, t, null);
    }

    /**
     * 根据实体类对象和列簇构建put对象
     *
     * @param family    列簇
     * @param rowKey    行键
     * @param t         实体类对象
     * @param colPrefix 列名前缀
     * @param <T>       实体类对象的类型
     * @return 返回 Put 对象
     */
    public static <T> Put getPut(byte[] family, byte[] rowKey, T t, String colPrefix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Put put = new Put(rowKey);
        Set<Map.Entry<String, Class<?>>> entries = FieldClassUtils.getPrivateFieldClass(t.getClass()).entrySet();
        for (Map.Entry<String, Class<?>> entry : entries) {
            byte[] rowKeyBytes = Bytes.toBytes(entry.getKey());
            if (colPrefix != null && !"".equals(colPrefix)) {
                rowKeyBytes = Bytes.toBytes(colPrefix + entry.getKey());
            }

            Object invoke = getGetMethod(entry.getKey(), t.getClass()).invoke(t);

            switch (invoke.getClass().getSimpleName().toLowerCase()) {
                case "short":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((short) invoke));
                    break;
                case "integer":
                case "int":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((int) invoke));
                    break;
                case "long":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((long) invoke));
                    break;
                case "double":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((double) invoke));
                    break;
                case "float":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((float) invoke));
                    break;
                case "boolean":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((boolean) invoke));
                    break;
                case "string":
                    put.addColumn(family, rowKeyBytes, Bytes.toBytes((String) invoke));
                    break;
            }
        }
        return put;
    }

    /**
     * 获取类属性的getter方法
     *
     * @param cls       类
     * @param fieldName 属性名称
     */
    private static <T> Method getGetMethod(String fieldName, Class<T> cls) throws NoSuchMethodException {
        return cls.getMethod("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
    }

    /**
     * 获取类属性的setter方法
     *
     * @param cls       类
     * @param fieldName 属性名称
     * @param fieldCls  属性类型
     */
    private static <T> Method getSetMethod(String fieldName, Class<?> fieldCls, Class<T> cls) throws NoSuchMethodException {
        return cls.getMethod("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), fieldCls);
    }

    /**
     * 关闭admin、connection，清空conf
     */
    public static void close() {
        if (null != admin) {
            try {
                admin.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                admin = null;
            }
        }
        if (null != connection) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection = null;
            }
        }
        if (null != conf) {
            conf.clear();
            conf = null;
        }
    }

}
