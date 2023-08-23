package cn.whale.helper.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

public class DB {
    static Map<String, String> PG_GO_TYPE = new HashMap<>();
    static Map<String, String> PG_GO_TYPE_V2 = new HashMap<>();

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static {
        PG_GO_TYPE.put("varchar", "string");
        PG_GO_TYPE.put("int", "int32");
        PG_GO_TYPE.put("int2", "int32");
        PG_GO_TYPE.put("int4", "int32");
        PG_GO_TYPE.put("int8", "int64");
        PG_GO_TYPE.put("serial", "int64");
        PG_GO_TYPE.put("bigserial", "int64");
        PG_GO_TYPE.put("float4", "float32");
        PG_GO_TYPE.put("float8", "float64");
        PG_GO_TYPE.put("numeric", "decimal.Decimal");
        PG_GO_TYPE.put("bool", "bool");
        PG_GO_TYPE.put("_varchar", "pq.StringArray");
        PG_GO_TYPE.put("_int", "pq.Int32Array");
        PG_GO_TYPE.put("_int2", "pq.Int32Array");
        PG_GO_TYPE.put("_int4", "pq.Int32Array");
        PG_GO_TYPE.put("_int8", "pq.Int64Array");
        PG_GO_TYPE.put("_float4", "pq.Float32Array");
        PG_GO_TYPE.put("_float8", "pq.Float64Array");
        PG_GO_TYPE.put("jsonb", "postgres.Jsonb");
        PG_GO_TYPE.put("json", "string");
        PG_GO_TYPE.put("date", "*time.Time");
        PG_GO_TYPE.put("timestamp", "*time.Time");
        PG_GO_TYPE.put("timestamptz", "*time.Time");
    }


    static {
        PG_GO_TYPE_V2.put("varchar", "string");
        PG_GO_TYPE_V2.put("int", "int32");
        PG_GO_TYPE_V2.put("int2", "int32");
        PG_GO_TYPE_V2.put("int4", "int32");
        PG_GO_TYPE_V2.put("int8", "int64");
        PG_GO_TYPE_V2.put("serial", "int64");
        PG_GO_TYPE_V2.put("bigserial", "int64");
        PG_GO_TYPE_V2.put("float4", "float32");
        PG_GO_TYPE_V2.put("float8", "float64");
        PG_GO_TYPE_V2.put("numeric", "decimal.Decimal");
        PG_GO_TYPE_V2.put("bool", "bool");
        PG_GO_TYPE_V2.put("_varchar", "pq.StringArray");
        PG_GO_TYPE_V2.put("_int", "pq.Int32Array");
        PG_GO_TYPE_V2.put("_int2", "pq.Int32Array");
        PG_GO_TYPE_V2.put("_int4", "pq.Int32Array");
        PG_GO_TYPE_V2.put("_int8", "pq.Int64Array");
        PG_GO_TYPE_V2.put("_float4", "pq.Float32Array");
        PG_GO_TYPE_V2.put("_float8", "pq.Float64Array");
        PG_GO_TYPE_V2.put("jsonb", "datatypes.JSON");
        PG_GO_TYPE_V2.put("json", "string");
        PG_GO_TYPE_V2.put("date", "*time.Time");
        PG_GO_TYPE_V2.put("timestamp", "*time.Time");
        PG_GO_TYPE_V2.put("timestamptz", "*time.Time");
    }

    public static Connection getConnection(DbConfig config, String database) {
        try {
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(String.format("jdbc:postgresql://%s:%d/%s", config.host, config.port, database), config.user, config.password);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    static int doWithSql(ResultSetHandler resultSetHandler, DbConfig config, String database, String sql, Object... args) {
        try (Connection conn = getConnection(config, database); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.execute();
            int uc = ps.getUpdateCount();
            if (resultSetHandler == null) {
                return uc;
            }
            try (ResultSet rs = ps.getResultSet()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                List<String> cnames = new ArrayList<>(rsmd.getColumnCount());
                int c = rsmd.getColumnCount();
                for (int i = 1; i <= c; i++) {
                    cnames.add(rsmd.getColumnName(i).toLowerCase());
                }

                while (rs.next()) {
                    resultSetHandler.handleRow(rs, cnames);
                }
            }
            return uc;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void handleResultSet(ResultSet rs, ResultSetHandler handler) throws Exception {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<String> cnames = new ArrayList<>(rsmd.getColumnCount());
        int c = rsmd.getColumnCount();
        for (int i = 1; i <= c; i++) {
            cnames.add(rsmd.getColumnName(i).toLowerCase());
        }

        while (rs.next()) {
            handler.handleRow(rs, cnames);
        }
        safeClose(rs);
    }

    static void safeClose(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> getDatabases(DbConfig config) {
        final List<String> list = new ArrayList<>();
        doWithSql((rs, cnames) -> {
            list.add(rs.getString(1));
        }, config, "", "select datname from pg_database");
        Collections.sort(list);
        return list;
    }


    public static List<TableWithSchema> getTables(DbConfig config, String database) {
        final List<TableWithSchema> list = new ArrayList<>();
        doWithSql((rs, cnames) -> {
            var tb = new TableWithSchema();
            tb.tableName = rs.getString(1);
            tb.schema = rs.getString(2);
            list.add(tb);
        }, config, database, "select tablename,schemaname from pg_tables where schemaname <> 'information_schema' and schemaname not like 'pg_%' \n" +
                "and tablename  !~ '_copy\\d*$' \n" +
                "and tablename  !~ '_partition$' \n" +
                "and tablename  !~ '\\d{6,}$'\n" +
                "and tablename  !~ '^deprecated_'");
        Collections.sort(list);
        return list;
    }

    public static List<Column> getColumns(DbConfig config, String database, TableWithSchema table) {
        final List<Column> list = new ArrayList<>();
        try (Connection conn = DB.getConnection(config, database)) {
            ResultSet rs = conn.getMetaData().getColumns(null, table.schema, table.tableName, "%");
            handleResultSet(rs, (rs0, cnames) -> {
                Column col = new Column();
                col.name = rs0.getString("COLUMN_NAME");
                col.type = rs0.getInt("DATA_TYPE");
                col.typeName = rs0.getString("TYPE_NAME");
                col.comment = rs0.getString("REMARKS");
                col.size = rs0.getInt("COLUMN_SIZE");
                col.precision = rs0.getInt("DECIMAL_DIGITS");
                list.add(col);
            });
            rs = conn.getMetaData().getPrimaryKeys(null, table.schema, table.tableName);
            handleResultSet(rs, (rs0, cnames) -> {
                String pkc = rs0.getString("COLUMN_NAME");
                list.stream().filter(c -> c.name.equals(pkc)).forEach(c -> c.isPk = true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * get index list, without primary key index
     *
     * @param config
     * @param database
     * @param table
     * @return
     */
    public static List<Index> getIndexes(DbConfig config, String database, TableWithSchema table) {
        final List<Index> list = new ArrayList<>();
        doWithSql((rs, colNames) -> {
            Index idx = new Index();
            idx.name = rs.getString(2);
            idx.columns = List.of(rs.getString(3).split(","));
            idx.isUnique = rs.getBoolean(4);
            idx.indexType = rs.getString(5);
            list.add(idx);
        }, config, database, "select\n" +
                "    t.relname as table_name,\n" +
                "    i.relname as index_name,\n" +
                "    array_to_string(array_agg(a.attname), ',') as column_names,\n" +
                "    ix.indisunique,\n" +
                "    max(am.amname) as index_type\n" +
                "from\n" +
                "    pg_class t,\n" +
                "    pg_class i,\n" +
                "    pg_index ix,\n" +
                "    pg_attribute a,\n" +
                "    pg_am am\n" +
                "where\n" +
                "    t.oid = ix.indrelid\n" +
                "    and i.oid = ix.indexrelid\n" +
                "    and a.attrelid = t.oid\n" +
                "    and a.attnum = ANY(ix.indkey)\n" +
                "    and am.oid=i.relam\n" +
                "    and t.relkind = 'r'\n" +
                "    and t.relname =?\n" +
                "    and ix.indisprimary=false\n" +
                "group by\n" +
                "    t.relname,\n" +
                "    i.relname,\n" +
                "    ix.indisunique\n" +
                "order by\n" +
                "    t.relname,\n" +
                "    i.relname;", table.tableName);

        return list;
    }

    public static String pgToGoType(String pGtype) {
        return PG_GO_TYPE.getOrDefault(pGtype, "string");
    }

    /**
     * for gorm v2
     *
     * @param pGtype
     * @return
     */
    public static String pgToGoTypeV2(String pGtype) {
        return PG_GO_TYPE_V2.getOrDefault(pGtype, "string");
    }

    interface ResultSetHandler {
        void handleRow(ResultSet rs, List<String> colNames) throws Exception;
    }

    public static class TableWithSchema implements Comparable<TableWithSchema> {
        public String tableName = "";
        public String schema = "";

        @Override
        public String toString() {
            return tableName;
        }

        @Override
        public int compareTo(@NotNull DB.TableWithSchema o) {
            return this.tableName.compareTo(o.tableName);
        }
    }


    public static class Column {
        /**
         * column name
         */
        public String name;
        /**
         * sql type
         */
        public int type;
        /**
         * column type name
         */
        public String typeName;
        public boolean isPk;
        public String comment;

        public int size;
        public int precision;


        @Override
        public String toString() {
            return "Column{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", typeName='" + typeName + '\'' +
                    ", isPk=" + isPk +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }

    public static class Index {
        public String name;
        public boolean isUnique;

        public String indexType;
        public List<String> columns;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");
            sb.append(StringUtils.join(columns, ", "));
            sb.append(")");
            if (isUnique){
                sb.append(" unique");
            }
            if("btree".equalsIgnoreCase(indexType)) {
                sb.append(" ").append(indexType);
            }
            return sb.toString();
        }
    }


}
