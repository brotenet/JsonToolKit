package openjdk.tools.json.util.converters;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;

public class JsonSqlConverter {

	public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
	public static String DEFAULT_TIME_FORMAT = "HH:mm:ss";
	public static String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	public static JsonList convert(ResultSet result_set, String date_format, String time_format, String timestamp_format) throws SQLException, JsonException {
		JsonList output = new JsonList();
		ResultSetMetaData meta_data = result_set.getMetaData();
		int number_of_columns = meta_data.getColumnCount();
		while (result_set.next()) {
			JsonMap item = new JsonMap();
			for (int i = 1; i < number_of_columns + 1; i++) {
				String column_name = meta_data.getColumnName(i);
				try {
					if (meta_data.getColumnType(i) == java.sql.Types.ARRAY) {
						item.put(column_name, result_set.getArray(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.BIGINT) {
						item.put(column_name, result_set.getLong(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.REAL) {
						item.put(column_name, result_set.getFloat(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.BOOLEAN) {
						item.put(column_name, result_set.getBoolean(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.BLOB) {
						item.put(column_name, result_set.getBlob(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.DOUBLE) {
						item.put(column_name, result_set.getDouble(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.FLOAT) {
						item.put(column_name, result_set.getDouble(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.INTEGER) {
						item.put(column_name, result_set.getInt(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.NVARCHAR) {
						item.put(column_name, result_set.getNString(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.VARCHAR) {
						item.put(column_name, result_set.getString(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.CHAR) {
						item.put(column_name, result_set.getString(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.NCHAR) {
						item.put(column_name, result_set.getString(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.LONGNVARCHAR) {
						item.put(column_name, result_set.getString(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.LONGVARCHAR) {
						item.put(column_name, result_set.getString(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.TINYINT) {
						item.put(column_name, result_set.getByte(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.SMALLINT) {
						item.put(column_name, result_set.getShort(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.DATE) {
						item.put(column_name, formatSqlDate(result_set.getDate(column_name), date_format));
					} else if (meta_data.getColumnType(i) == java.sql.Types.TIME) {
						item.put(column_name, formatSqlTime(result_set.getTime(column_name), time_format));
					} else if (meta_data.getColumnType(i) == java.sql.Types.TIMESTAMP) {
						item.put(column_name, formatSqlTimeStamp(result_set.getTimestamp(column_name), timestamp_format));
					} else if (meta_data.getColumnType(i) == java.sql.Types.BINARY) {
						item.put(column_name, result_set.getBytes(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.VARBINARY) {
						item.put(column_name, result_set.getBytes(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.LONGVARBINARY) {
						item.put(column_name, result_set.getBinaryStream(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.BIT) {
						item.put(column_name, result_set.getBoolean(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.CLOB) {
						item.put(column_name, result_set.getClob(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.NUMERIC) {
						item.put(column_name, result_set.getBigDecimal(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.DECIMAL) {
						item.put(column_name, result_set.getBigDecimal(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.DATALINK) {
						item.put(column_name, result_set.getURL(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.REF) {
						item.put(column_name, result_set.getRef(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.STRUCT) {
						item.put(column_name, result_set.getObject(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.DISTINCT) {
						item.put(column_name, result_set.getObject(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.JAVA_OBJECT) {
						item.put(column_name, result_set.getObject(column_name));
					} else if (meta_data.getColumnType(i) == java.sql.Types.NULL) {
						item.put(column_name, JsonMap.NULL);
					} else {
						if(meta_data.getColumnTypeName(i).equalsIgnoreCase("json") || meta_data.getColumnTypeName(i).equalsIgnoreCase("jsonb")) {
							if(result_set.getString(i).trim().startsWith("{")) {
								item.put(column_name, new JsonMap(result_set.getString(i)));
							}else if(result_set.getString(i).trim().startsWith("[")) {
								item.put(column_name, new JsonList(result_set.getString(i)));
							}else {
								item.put(column_name, result_set.getString(i));
							}
						}else {
							item.put(column_name, result_set.getString(i));
						}
					}
				}catch (Exception e) {
					item.put(column_name, String.valueOf(result_set.getObject(i)));
				}
			}
			output.put(item);
		}
		return output;
	}
	
	public static JsonList convert(ResultSet result_set) throws SQLException, JsonException{
		return convert(result_set, DEFAULT_DATE_FORMAT, DEFAULT_TIME_FORMAT, DEFAULT_TIMESTAMP_FORMAT);
	}

	public static String formatSqlDate(Date date, String date_format) {
		SimpleDateFormat formater = new SimpleDateFormat(date_format);
		return formater.format(date);
	}

	public static String formatSqlTime(Time time, String time_format) {
		SimpleDateFormat formater = new SimpleDateFormat(time_format);
		return formater.format(time);
	}

	public static String formatSqlTimeStamp(Timestamp timestamp, String timestamp_format) {
		SimpleDateFormat formater = new SimpleDateFormat(timestamp_format);
		return formater.format(timestamp);
	}
	
	public static JsonList query(Connection connection, String sql, String date_format, String time_format, String timestamp_format) throws SQLException, JsonException{
		Statement statement = connection.createStatement();
		return JsonSqlConverter.convert(statement.executeQuery(sql), date_format, time_format, timestamp_format);
	}
	
	public static JsonList query(Connection connection, String sql) throws SQLException, JsonException{
		return query(connection, sql, JsonSqlConverter.DEFAULT_DATE_FORMAT, JsonSqlConverter.DEFAULT_TIME_FORMAT, JsonSqlConverter.DEFAULT_TIMESTAMP_FORMAT);
	}
	
	public static JsonList query(String url, String jdbc_driver, String user, String password, String sql, String date_format, String time_format, String timestamp_format) throws ClassNotFoundException, SQLException, JsonException {
		Class.forName(jdbc_driver);
		Connection connection = DriverManager.getConnection(url, user, password);
		return query(connection, sql, date_format, time_format, timestamp_format);
	}
	
	public static JsonList query(String url, String jdbc_driver, String user, String password, String sql) throws ClassNotFoundException, JsonException, SQLException{
		return query(url, jdbc_driver, user, password, sql, JsonSqlConverter.DEFAULT_DATE_FORMAT, JsonSqlConverter.DEFAULT_TIME_FORMAT, JsonSqlConverter.DEFAULT_TIMESTAMP_FORMAT);
	}
	
	public static int update(Connection connection, String sql) throws SQLException {
		Statement statement = connection.createStatement();
		return statement.executeUpdate(sql);
	}
	
	public static int update(String url, String jdbc_driver, String user, String password, String sql) throws ClassNotFoundException, SQLException {
		Class.forName(jdbc_driver);
		Connection connection = DriverManager.getConnection(url, user, password);
		return update(connection, sql);
	}
	
	public static long largeUpdate(Connection connection, String sql) throws SQLException {
		Statement statement = connection.createStatement();
		return statement.executeLargeUpdate(sql);
	}
	
	public static long largeUpdate(String url, String jdbc_driver, String user, String password, String sql) throws ClassNotFoundException, SQLException {
		Class.forName(jdbc_driver);
		Connection connection = DriverManager.getConnection(url, user, password);
		return largeUpdate(connection, sql);
	}

}
