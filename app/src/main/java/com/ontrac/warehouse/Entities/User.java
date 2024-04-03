package com.ontrac.warehouse.Entities;

import com.ontrac.warehouse.OnTrac.APIs;
import com.ontrac.warehouse.Utilities.Strings;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class User extends RealmObject {
    @PrimaryKey
    public int Id;
    public String Name;
    public String Password;
    public String FacilityCode;

    public User() {}

    public User(APIs.UserInfoObject item) {
        Id = item.UserId;
        Name = item.UserName;
        Password = item.Password;
        FacilityCode = item.FaciliyCode;
    }

    public String FriendlyName() {
        String result = Name;

        String[] parts = result.split(",");

        switch(parts.length){
            case 1 :
                result = Strings.CapitalizeFirst(parts[0]);
                break;
            case 2 :
                result = Strings.CapitalizeFirst(parts[1].trim()) + " " + Strings.CapitalizeFirst(parts[0].trim());
                break;
            default :
                break;
        }

        return result;
    }

    public static User Clone(User user) {
        User result = new User();
        result.Id = user.Id;
        result.Name = user.Name;
        result.Password = user.Password;
        result.FacilityCode = user.FacilityCode;

        return result;
    }

    public static Thread Insert(int id, String name, String password, String facilityCode){
        final User item = new User();
        item.Id = id;
        item.Name = name;
        item.Password = password;
        item.FacilityCode = facilityCode;

        Thread t = new Thread(new Runnable() {
            public void run() {
                Realm realm = Realm.getDefaultInstance();

                realm.beginTransaction();
                realm.copyToRealm(item);
                realm.commitTransaction();

                if (!realm.isClosed()) {
                    realm.close();
                }
            }
        });
        t.start();

        return t;
    }

    public static User Find(int id){
        User result = null;

        Realm realm = Realm.getDefaultInstance();

        RealmQuery<User> query = realm.where(User.class);
        final RealmResults<User> results = query.equalTo("Id", id).findAll();

        if (results.size() > 0)
        {
            result = results.first();
        }

        return result;
    }

    public static AuthenticateResult Authenticate(int id, String password)
    {
        AuthenticateResult result = new AuthenticateResult();

        User user = Find(id);

        if (user == null)
        {
            result.UserNotFound = true;
        }
        else {
            result.User = user;
            result.PasswordMismatch = !user.Password.equals(password);

            if (!result.PasswordMismatch)
            {
                result.Authorized = true;
            }
        }

        return result;
    }

    public static class AuthenticateResult{
        public Boolean Authorized = false;
        public Boolean UserNotFound = false;
        public Boolean PasswordMismatch = false;
        public User User = null;
    }

    public static void DeleteAll(){
        Realm realm = Realm.getDefaultInstance();

        RealmResults<User> results = realm.where(User.class).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }
    }

    public static long Count(){
        Realm realm = Realm.getDefaultInstance();
        return realm.where(User.class).count();
    }

    public static int Save(APIs.UserInfoObject[] items){
        int recordCount = 0;

        Realm realm = Realm.getDefaultInstance();

        // delete existing data
        //RealmQuery<User> query = realm.where(User.class);
        //final RealmResults<User> allUsers = query.findAll();
        //RealmResults<User> allUsers = realm.where(User.class).findAll();

        /*
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                allUsers.deleteAllFromRealm();
            }
        });
        */

        realm.beginTransaction();

        //allUsers.deleteAllFromRealm();

        for (APIs.UserInfoObject item: items) {
            realm.copyToRealm(new User(item));
            recordCount += 1;
        }

        realm.commitTransaction();

        if (!realm.isClosed()) {
            realm.close();
        }

        return recordCount;
    }
}
