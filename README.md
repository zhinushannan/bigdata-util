<div align="center">
  <h1>📀bigdata-util</h1>
  <span>🇬🇧English</span>  <a href="https://github.com/zhinushannan/bigdata-util/blob/main/README_zh.md">🇨🇳简体中文</a>
</div>




# ✨Project Description

If you are using or learning big data, this tool framework can reduce your code amount by 60%, and can also improve the running speed to a certain extent.

Features include:

- Hadoop：Not yet written
- HBase：
  - Acquisition and release of HBase configuration objects, connection objects, and Admin objects
  - Deletion of namespaces
  - Deletion of tables
  - Convert scan results to entity class objects (can be batched)
  - Entity class object to Put object
  - 【pending upgrade】

> Tips：This tool has provided `hadoop-common-3.3.2`, `hadoop-client-3.3.2`, `hadoop-hdfs-3.3.2`, `hbase-client-2.4.8`, `hbase-server-2.4.8` , `hbase-mapreduce-2.4.8` dependencies, to meet the basic development needs, you do not need to add manually.

# 🧾Api Docs

### `HBaseUtils`

- Base object

  - `public static Configuration getConf()`：Get HBase configuration object
  - `public static Connection getConnection()`：Get HBase connection object
  - `public static Admin getAdmin()`：Get HBase Admin object
  - `public static void close()`：Close the connection object and the Admin object

- Namespace and table deletion

  - `public static void deleteNamespaceIfExist(String namespace)`：If the specified namespace exists, delete the namespace

  - `public static void deleteTaleIfExist(TableName tableName)`：If the specified table exists, delete the table

- Get value

  - ~~`public static Method getBytesToMethod(Class<?> cls)`~~：Obtain the corresponding conversion method according to the data type, such as passing in `int.class`, and returning the `Bytes.toInt(byte[])` method (because this method does not need to be exposed, the next version will be set to private)

  - `public static Object getColVal(Class<?> cls, Result result, byte[] family, byte[] col)`：Get the corresponding value based on the data type, result set, and column name in the column cluster.

  - `public static <T> T getInstance(Class<T> cls, Result result, byte[] family)`：Get the object according to the scan result Cell (no column name prefix)

  - `public static <T> T getInstance(Class<T> cls, Result result, byte[] family, String colPrefix)`：Obtain the object according to the scan result Cell (specify the prefix of the column name, if the specified prefix is `test_`, the prefix `test_` of the column name will be removed in the process of obtaining the data from the table and converting it to the entity class, **Note: the column name The prefix `null` and `""` are not the same!**)

  - `public static <T> List<T> getInstances(Class<T> cls, ResultScanner results, byte[] family)`：Obtain a list of objects based on the scan result set ResultScanner (without column name prefix)

  - `public static <T> List<T> getInstances(Class<T> cls, ResultScanner results, byte[] family, String colPrefix)`：Obtain a list of objects based on the scan result set ResultScanner (excluding the specified column name prefix, if the specified prefix is `test_`, the column name prefix `test_` will be removed in the process of obtaining data from the table and converting it to an entity class, **Note : column name prefixes `null` and `""` are not the same!**)

- Get the `Put` object

  - `public static <T> Put getPut(byte[] row, byte[] family, T t)`：Build put objects from entity class objects and column clusters (without column name prefixes)
  - `public static <T> Put getPut(byte[] row, byte[] family, T t, String colPrefix)`：Build the put object based on the entity class object and column cluster (specify the column name prefix, if the specified prefix is `test_`, the column name stored in the table will have the prefix `test_`, **Note: the column name prefixes `null` and ` ""`Not the same!**)

# 🥄Instructions

### Import dependencies

You need to add the following dependencies to the `pom.xml` file:

```xml
    <dependency>
      <groupId>io.github.zhinushannan</groupId>
      <artifactId>bigdata-util</artifactId>
      <version>0.0.2-RELEASE</version>
    </dependency>
```

### Pre-preparation

All the following demos take the following entity class as an example:

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

At the same time, you need to copy your HBase configuration file `hbase.site.xml` to the `src/main/resources` directory.

### HBase create namespace

#### Code that does not use the tool framework

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

#### Code using the tool framework

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

### HBase create table

#### Code that does not use the tool framework

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

#### Code using the tool framework

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

### Store data in HBase

#### Code that does not use the tool framework

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

#### Code using the tool framework

illustrate：

1. `HBaseUtils.getPut()The parameters that need to be passed are: the byte array of the row key, the byte array of the column family, and the object to be stored.
2. The column name stored in the table is the attribute name of the class.

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

### HBase read data

**Note: Please execute the following example code after executing the above code for storing data. **

#### Code that does not use the tool framework

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

#### Code using the tool framework

The framework provides three output modes:

- `HBaseUtils.getInstance()`：The parameters that need to be passed are the class of the entity class, the single result set Cell, and the byte array of the column family. This method returns the object corresponding to the Cell.
- `HBaseUtils.getInstances()`：The parameters that need to be passed are the class of the entity class, the scan result set, and the byte array of the column family. This method returns a list of all objects corresponding to the result set.
- `HBaseUtils.show()`：The parameters that need to be passed are the class of the entity class, the scan result set, and the byte array of the column family. This method has no return value, and directly calls the `toString()` method of the object to output.

The first two formats provide more operation space and can process the objects corresponding to the returned result set.

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

            // Method 1: Traverse scan, get each object in turn and output
            ResultScanner results = demo.getScanner(scan);
            for (Result result : results) {
                PersonWritable instance = HBaseUtils.getInstance(PersonWritable.class, result, family);
                System.out.println(instance);
            }

            // Method 2: Directly obtain all objects in the result set and output them
            results = demo.getScanner(scan);
            List<PersonWritable> instances = HBaseUtils.getInstances(PersonWritable.class, results, family);
            System.out.println(instances);

            // Method 3: Direct output
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



# Changelog

- 2022/07/29: HBase tool class, version: 0.0.1-SNAPSHOT
- 2022/07/30:  The Hbase tool class allows custom prefixes to obtain Put objects from entity class objects and to obtain entity class objects from scan result sets. Version: 0.0.2-SNAPSHOT
