package dk.trustworks.userservice.repositories;

import dk.trustworks.userservice.model.Salary;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class SalaryRepository {

    private JDBCClient jdbc;

    public SalaryRepository(JDBCClient jdbc) {
        this.jdbc = jdbc;
    }

    private Single<SQLConnection> connect() {
        return jdbc.rxGetConnection()
                .map(c -> c.setOptions(new SQLOptions().setAutoGeneratedKeys(true)));
    }

    public Single<List<Salary>> getAllUserSalaries(String... userUUIDs) {
        return connect().flatMap((SQLConnection connection) -> connection.rxQueryWithParams(
                "SELECT * FROM salary where useruuid IN ?",
                new JsonArray().add(userUUIDs))
                .map(rs -> rs.getRows().stream().map(Salary::new).collect(Collectors.toList()))
                .doFinally(connection::close));
    }

    public Completable create(String useruuid, Salary salary) {
        return connect().flatMapCompletable(connection -> {
            String sql = "INSERT INTO usermanager.salary " +
                    "(uuid, useruuid, salary, activefrom) " +
                    "VALUES " +
                    "(?, ?, ?, ?);";
            JsonArray params = new JsonArray()
                    .add(salary.getUuid())
                    .add(useruuid)
                    .add(salary.getSalary())
                    .add(salary.getActivefrom().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            return connection.rxUpdateWithParams(sql, params)
                    .flatMapCompletable(ur ->
                            ur.getUpdated() == 0 ?
                                    Completable.error(new NoSuchElementException("No user with uuid " + useruuid))
                                    : Completable.complete()
                    )
                    .doFinally(connection::close);
        });
    }

    public Completable delete(String salaryuuid) {
        System.out.println("SalaryRepository.delete");
        System.out.println("salaryuuid = " + salaryuuid);
        return connect().flatMapCompletable(connection -> {
            String sql = "DELETE FROM usermanager.salary WHERE uuid LIKE ?;";
            System.out.println("sql = " + sql);
            JsonArray params = new JsonArray().add(salaryuuid);
            System.out.println("salaryuuid = " + salaryuuid);
            return connection.rxUpdateWithParams(sql, params)
                    .flatMapCompletable(ur ->
                            ur.getUpdated() == 0 ?
                                    Completable.error(new NoSuchElementException("No salary with uuid " + salaryuuid))
                                    : Completable.complete()
                    )
                    .doFinally(connection::close);
        });
    }
}