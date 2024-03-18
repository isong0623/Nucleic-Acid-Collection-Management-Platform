package com.dreaming.hscj;

public class Constants {
    public static class User{

        private static String account = "";
        public static void setAccount(String account){
            User.account = account;
        }
        public static String getAccount() {
            return account;
        }

        private static boolean bAutoLogin;
        public static void setAutoLogin(boolean bAutoLogin){
            User.bAutoLogin = bAutoLogin;
        }
        public static boolean isAutoLogin() {
            return bAutoLogin;
        }

        private static boolean bRememberPassword;
        public static void setRememberPassword(boolean bRememberPassword){
            User.bRememberPassword = bRememberPassword;
        }
        public static boolean isRememberPassword() {
            return bRememberPassword;
        }

        private static String password = "";
        public static void setPassword(String password){
            User.password = password;
        }
        public static String getPassword() {
            return password;
        }

        public static boolean isLogin() {
            return System.currentTimeMillis() < getExpired();
        }

        private static String token = "";
        public static void setToken(String token){
            User.token = token;
        }
        public static String getToken() {
            return token;
        }

        private static long expired = 0L;
        public static void setExpired(long expired){
            User.expired = expired;
        }
        public static long getExpired(){
            return expired;
        }
    }
}
