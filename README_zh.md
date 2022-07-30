<div align="center">
  <h1>📀bigdata-util</h1>
  <a href="https://github.com/zhinushannan/bigdata-util/blob/main/README.md">🇬🇧English</a>  <span>🇨🇳简体中文</span>
</div>

# ✨项目简介

如果你在使用或者学习大数据，该工具框架可以减少您**60%**的代码量，也可以提升一定程度的运行速度。

功能包括：

- Hadoop：暂未编写
- HBase：
  - HBase配置对象、连接对象、Admin对象的获取与释放
  - 命名空间的删除
  - 表的删除
  - 扫描结果转实体类对象（可批量）
  - 实体类对象转Put对象
  - 【待更新】

> 提示：本工具已提供`hadoop-common-3.3.2`、`hadoop-client-3.3.2`、`hadoop-hdfs-3.3.2`、`hbase-client-2.4.8`、`hbase-server-2.4.8`、`hbase-mapreduce-2.4.8`依赖，满足基本开发需求，可以不需要手动添加。

# 🧾接口简介

### `HBaseUtils`

- 基础对象

  - `public static Configuration getConf()`：获取HBase配置对象

  - `public static Connection getConnection()`：获取HBase连接对象

  - `public static Admin getAdmin()`：获取HBase Admin对象
  - `public static void close()`：关闭连接对象和Admin对象

- 命名空间与表的删除

  - `public static void deleteNamespaceIfExist(String namespace)`：如果指定命名空间存在，则删除命名空间

  - `public static void deleteTaleIfExist(TableName tableName)`：如果指定表存在，则删除表

- 值的获取

  - ~~`public static Method getBytesToMethod(Class<?> cls)`~~：根据数据类型获取对应的转换方法，如传入`int.class`，返回的是`Bytes.toInt(byte[])`方法（由于此方法不需要对外暴露，下个版本将设为private）

  - `public static Object getColVal(Class<?> cls, Result result, byte[] family, byte[] col)`：根据数据类型、结果集和列簇中的列名获取对应的值。

  - `public static <T> T getInstance(Class<T> cls, Result result, byte[] family)`：根据扫描结果Cell得到对象（无列名前缀）

  - `public static <T> T getInstance(Class<T> cls, Result result, byte[] family, String colPrefix)`：根据扫描结果Cell得到对象（指定列名前缀，如指定前缀为`test_`，则从表中获取数据转为实体类的过程中将列名的前缀`test_`移除，**注意：列名前缀`null`和`""`不相同！**）

  - `public static <T> List<T> getInstances(Class<T> cls, ResultScanner results, byte[] family)`：根据扫描结果集ResultScanner获得对象列表（无列名前缀）

  - `public static <T> List<T> getInstances(Class<T> cls, ResultScanner results, byte[] family, String colPrefix)`：根据扫描结果集ResultScanner获得对象列表（排除指定列名前缀，如指定前缀为`test_`，则从表中获取数据转为实体类的过程中将列名的前缀`test_`移除，**注意：列名前缀`null`和`""`不相同！**）

- `Put`对象的获取

  - `public static <T> Put getPut(byte[] row, byte[] family, T t)`：根据实体类对象和列簇构建put对象（无列名前缀）
  - `public static <T> Put getPut(byte[] row, byte[] family, T t, String colPrefix)`：根据实体类对象和列簇构建put对象（指定列名前缀，如指定前缀为`test_`，存入表中的列名便拥有前缀`test_`，**注意：列名前缀`null`和`""`不相同！**）

# 🥄使用方法

### 导入依赖

您需要在`pom.xml`文件中添加如下依赖：

```xml
    <dependency>
      <groupId>io.github.zhinushannan</groupId>
      <artifactId>bigdata-util</artifactId>
      <version>0.0.1-RELEASE</version>
    </dependency>
```

### 前置准备

如下所有Demo以如下实体类为例：

```java
package org.example.hbase.writable;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PersonWritable implements Writable {

    private String name;
    private Integer age;

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.name);
        dataOutput.writeInt(this.age);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.name = dataInput.readUTF();
        this.age = dataInput.readInt();
    }

    @Override
    public String toString() {
        return "[name=" + this.name + ", age=" + this.age + "]";
    }

    public PersonWritable() {
    }

    public PersonWritable(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

}
```

同时，您需要将您的HBase配置文件`hbase.site.xml`拷贝至`src/main/resources`目录下。

### HBase创建命名空间

#### 不使用该工具框架的代码

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CreateNamespace {

    public static void main(String[] args) {

        Configuration conf;
        Connection connection = null;
        Admin admin = null;
        try {
            conf = HBaseConfiguration.create();
            connection = ConnectionFactory.createConnection(conf);
            admin = connection.getAdmin();

            NamespaceDescriptor demo = NamespaceDescriptor.create("demo").build();
            List<String> namespaces = Arrays.stream(admin.listNamespaces()).collect(Collectors.toList());
            if (namespaces.contains(demo.getName())) {
                TableName[] tableNames = admin.listTableNamesByNamespace(demo.getName());
                for (TableName tableName : tableNames) {
                    admin.disableTable(tableName);
                    admin.deleteNamespace(tableName.getNameAsString());
                }
                admin.deleteNamespace(demo.getName());
            }

            admin.createNamespace(demo);

            System.out.println(Arrays.toString(admin.listNamespaces()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != admin) {
                try {
                    admin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != connection) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

#### 使用该工具框架的代码

```java
import io.github.zhinushannan.util.hbase.HBaseUtils;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.client.Admin;

import java.io.IOException;
import java.util.Arrays;

public class CreateNamespace {
    public static void main(String[] args) {
        try {
            Admin admin = HBaseUtils.getAdmin();

            NamespaceDescriptor demo = NamespaceDescriptor.create("demo").build();
            HBaseUtils.deleteNamespaceIfExist(demo.getName());
            admin.createNamespace(demo);

            System.out.println(Arrays.toString(admin.listNamespaces()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HBaseUtils.close();
        }
    }
}
```

### HBase创建表

#### 不实用该框架的代码

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class CreateTable {
    public static void main(String[] args) {
        Configuration conf;
        Connection connection = null;
        Admin admin = null;
        try {
            conf = HBaseConfiguration.create();
            connection = ConnectionFactory.createConnection(conf);
            admin = connection.getAdmin();
            TableDescriptor build = TableDescriptorBuilder.newBuilder(TableName.valueOf(Bytes.toBytes("demo"))).setColumnFamily(ColumnFamilyDescriptorBuilder.of("demo")).build();
            if (admin.tableExists(build.getTableName())) {
                admin.disableTable(build.getTableName());
                admin.deleteTable(build.getTableName());
            }
            admin.createTable(build);

            for (TableName tableName : admin.listTableNames()) {
                System.out.println(tableName.getNameAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != admin) {
                try {
                    admin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != connection) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

#### 使用该框架的代码

```java
import io.github.zhinushannan.util.hbase.HBaseUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class CreateTable {
    public static void main(String[] args) {
        try {
            TableDescriptor build = TableDescriptorBuilder.newBuilder(TableName.valueOf(Bytes.toBytes("demo"))).setColumnFamily(ColumnFamilyDescriptorBuilder.of("demo")).build();
            HBaseUtils.deleteTaleIfExist(build.getTableName());
            HBaseUtils.getAdmin().createTable(build);
            for (TableName tableName : HBaseUtils.getAdmin().listTableNames()) {
                System.out.println(tableName.getNameAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HBaseUtils.close();
        }
    }
}
```

### HBase存入数据

#### 不实用该工具框架的代码

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.example.hbase.writable.PersonWritable;

import java.io.IOException;

public class PersonPut {

    public static void main(String[] args) {
        PersonWritable person1 = new PersonWritable("zhangsan", 18);
        PersonWritable person2 = new PersonWritable("lisi", 19);
        PersonWritable person3 = new PersonWritable("lilei", 20);
        PersonWritable person4 = new PersonWritable("hanmeimei", 21);

        Configuration conf;
        Connection connection = null;
        try {
            conf = HBaseConfiguration.create();
            connection = ConnectionFactory.createConnection(conf);

            Table demo = connection.getTable(TableName.valueOf(Bytes.toBytes("demo")));
            byte[] family = Bytes.toBytes("demo");
            byte[] nameCol = Bytes.toBytes("name");
            byte[] ageCol = Bytes.toBytes("age");

            Put put1 = new Put(Bytes.toBytes("person1"));
            put1.addColumn(family, nameCol, Bytes.toBytes(person1.getName()));
            put1.addColumn(family, ageCol, Bytes.toBytes(person1.getAge()));
            demo.put(put1);

            Put put2 = new Put(Bytes.toBytes("person2"));
            put2.addColumn(family, nameCol, Bytes.toBytes(person2.getName()));
            put2.addColumn(family, ageCol, Bytes.toBytes(person2.getAge()));
            demo.put(put2);

            Put put3 = new Put(Bytes.toBytes("person3"));
            put3.addColumn(family, nameCol, Bytes.toBytes(person3.getName()));
            put3.addColumn(family, ageCol, Bytes.toBytes(person3.getAge()));
            demo.put(put3);

            Put put4 = new Put(Bytes.toBytes("person4"));
            put4.addColumn(family, nameCol, Bytes.toBytes(person4.getName()));
            put4.addColumn(family, ageCol, Bytes.toBytes(person4.getAge()));
            demo.put(put4);

            demo.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

#### 使用该工具框架的代码

说明：

1. `HBaseUtils.getPut()`需要传递的参数分别为：行键的byte数组、列簇的byte数组、需要存入的对象。
2. 存入表中的列名为类的属性名。

```java
import io.github.zhinushannan.util.hbase.HBaseUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.example.hbase.writable.PersonWritable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class PersonPut {
    public static void main(String[] args) {

        PersonWritable person1 = new PersonWritable("zhangsan", 18);
        PersonWritable person2 = new PersonWritable("lisi", 19);
        PersonWritable person3 = new PersonWritable("lilei", 20);
        PersonWritable person4 = new PersonWritable("hanmeimei", 21);

        try {
            Table demo = HBaseUtils.getConnection().getTable(TableName.valueOf(Bytes.toBytes("demo")));
            byte[] family = Bytes.toBytes("demo");

            Put put1 = HBaseUtils.getPut(Bytes.toBytes("person1"), family, person1);
            demo.put(put1);

            Put put2 = HBaseUtils.getPut(Bytes.toBytes("person2"), family, person2);
            demo.put(put2);

            Put put3 = HBaseUtils.getPut(Bytes.toBytes("person3"), family, person3);
            demo.put(put3);

            Put put4 = HBaseUtils.getPut(Bytes.toBytes("person4"), family, person4);
            demo.put(put4);

            demo.close();
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            HBaseUtils.close();
        }
    }
}
```

### HBase读数据

**注意：请在执行完上述存入数据的代码后执行如下实例代码。**

#### 不使用该工具框架的代码

```java
import io.github.zhinushannan.util.hbase.HBaseUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class PersonRead {
    public static void main(String[] args) {
        Configuration conf;
        Connection connection = null;
        try {
            conf = HBaseUtils.getConf();
            connection = ConnectionFactory.createConnection(conf);

            Table demo = connection.getTable(TableName.valueOf("demo"));
            Scan scan = new Scan();
            ResultScanner results = demo.getScanner(scan);

            byte[] family = Bytes.toBytes("demo");
            byte[] nameCol = Bytes.toBytes("name");
            byte[] ageCol = Bytes.toBytes("age");

            for (Result result : results) {
                String row = Bytes.toString(result.getRow());
                String name = Bytes.toString(result.getValue(family, nameCol));
                int age = Bytes.toInt(result.getValue(family, ageCol));
                System.out.println("row=" + row + ", name=" + name + ", age=" + age);
            }

            demo.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

#### 使用该工具框架的代码

本框架提供三种输出模式：

- `HBaseUtils.getInstance()`：需要传递的参数分别为实体类的class、单个结果集Cell、列簇的byte数组。该方法返回的是该Cell对应的对象。
- `HBaseUtils.getInstances()`：需要传递的参数分别为实体类的class、扫描结果集、列簇的byte数组。该方法返回的是该结果集对应的所有对象的列表。
- `HBaseUtils.show()`：需要传递的参数分别为实体类的class、扫描结果集、列簇的byte数组。该方法没有返回值，直接调用对象的`toString()`方法进行输出。

其中前两种格式提供了更多的操作空间，可以对返回的结果集对应的对象进行处理。

```java
import io.github.zhinushannan.util.hbase.HBaseUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.example.hbase.writable.PersonWritable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PersonRead {
    public static void main(String[] args) {
        try {
            Table demo = HBaseUtils.getConnection().getTable(TableName.valueOf("demo"));
            Scan scan = new Scan();

            byte[] family = Bytes.toBytes("demo");

            // 方法一：遍历scan，依次获取每一个对象并输出
            ResultScanner results = demo.getScanner(scan);
            for (Result result : results) {
                PersonWritable instance = HBaseUtils.getInstance(PersonWritable.class, result, family);
                System.out.println(instance);
            }

            // 方法二：直接获取结果集中所有对象并进行输出
            results = demo.getScanner(scan);
            List<PersonWritable> instances = HBaseUtils.getInstances(PersonWritable.class, results, family);
            System.out.println(instances);

            // 方法三：直接输出
            results = demo.getScanner(scan);
            HBaseUtils.show(PersonWritable.class, results, family);

            demo.close();
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InstantiationException |
                 InvocationTargetException e) {
            e.printStackTrace();
        } finally {
            HBaseUtils.close();
        }
    }
}
```



# 更新日志

- 2022/07/29：HBase工具类，版本：0.0.1-SNAPSHOT
- 2022/07/30：Hbase工具类中根据实体类对象获取Put对象和根据扫描结果集获取实体类对象允许自定义前缀，版本：0.0.2-SNAPSHOT