package com.yu.yuaicodemother.constant;

import java.io.File;

public final class AppConstant {

        private static final String USER_DIR = System.getProperty("user.dir");

        public static final int GOOD_APP_PRIORITY = 99;

        public static final int DEFAULT_APP_PRIORITY = 0;


        public static final String CODE_OUTPUT_ROOT_DIR = USER_DIR + File.separator + "tmp" + File.separator
                        + "code_output"
                        + File.separator;

        public static final String CODE_DEPLOY_ROOT_DIR = USER_DIR + File.separator + "tmp" + File.separator
                        + "code_deploy"
                        + File.separator;

        public static final String CODE_DEPLOY_HOST = "http://localhost";

        private AppConstant() {
                throw new UnsupportedOperationException("Constant class cannot be instantiated");
        }

}
