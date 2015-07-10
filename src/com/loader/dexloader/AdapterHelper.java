package com.loader.dexloader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import android.content.Context;
import android.os.Build;
import dalvik.system.DexFile;

public class AdapterHelper {
    private static Object sNativeLibDirObj = null;
    private static Object sDexElementsObj = null;

    public static void initClassLoader(Context context, String zipFile,
            String zipPath, String nativeLibDir) {
        Log.d(Log.TAG, "");
        if (context == null) {
            return;
        }
        setExistObject(context);
        if (Build.VERSION.SDK_INT > 10) {
            addClassLoaderElementWithPathList(context, zipFile, zipPath,
                    nativeLibDir);
        } else if (Build.VERSION.SDK_INT == 10) {
            addClassLoaderElementWithoutPathList(context, zipFile,
                    nativeLibDir, zipPath);
        } else {
            addClassLoaderElement(context, zipFile, nativeLibDir);
        }
    }

    public static void addClassLoaderElementWithPathList(Context context,
            String nativeLibDir) {
        Log.d(Log.TAG, "");
        try {
            ClassLoader localClassLoader = context.getClassLoader();
            Class localClass1 = localClassLoader.getClass();
            Field localField = getFieldByName(localClass1, "pathList");
            if (localField == null)
                return;
            localField.setAccessible(true);
            Object localObject = localField.get(localClassLoader);
            Class localClass2 = localObject.getClass();
            addNativeLibDir(localClass2, localObject, nativeLibDir);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static void addClassLoaderElementWithPathList(Context context,
            String zipFile, String zipPath, String nativeLibDir) {
        Log.d(Log.TAG, "");
        try {
            ClassLoader localClassLoader = context.getClassLoader();
            Class localClass1 = localClassLoader.getClass();
            Field localField = getFieldByName(localClass1, "pathList");
            if (localField == null) {
                addClassLoaderElementWithoutPathList(context, zipFile,
                        nativeLibDir, zipPath);
                return;
            }
            localField.setAccessible(true);
            Object pathListObj = localField.get(localClassLoader);
            Class localClass2 = pathListObj.getClass();
            addDexFileAndElements(localClass2, pathListObj, zipFile, zipPath);
            addNativeLibDir(localClass2, pathListObj, nativeLibDir);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static void addClassLoaderElement(Context context, String zipFile,
            String nativeLibDir) {
        Log.d(Log.TAG, "");
        try {
            ClassLoader localClassLoader = context.getClassLoader();
            Class<? extends ClassLoader> localClass = localClassLoader
                    .getClass();
            Field localField1 = getFieldByName(localClass, "mFiles");
            if (localField1 == null) {
                return;
            }
            localField1.setAccessible(true);
            Object localObject1 = localField1.get(localClassLoader);
            int i = Array.getLength(localObject1);
            Object localObject2 = Array.newInstance(File.class, i + 1);
            for (int j = 0; j < i; j++) {
                Array.set(localObject2, j, Array.get(localObject1, j));
            }
            Array.set(localObject2, i, new File(zipFile));
            localField1.set(localClassLoader, localObject2);
            Field localField2 = getFieldByName(localClass, "mZips");
            if (localField2 == null) {
                return;
            }
            localField2.setAccessible(true);
            Object localObject3 = localField2.get(localClassLoader);
            i = Array.getLength(localObject3);
            Object localObject4 = Array.newInstance(ZipFile.class, i + 1);
            for (int k = 0; k < i; k++) {
                Array.set(localObject4, k, Array.get(localObject3, k));
            }
            Array.set(localObject4, i, new ZipFile(zipFile));
            localField2.set(localClassLoader, localObject4);
            Field localField3 = getFieldByName(localClass, "mDexs");
            if (localField3 == null) {
                return;
            }
            localField3.setAccessible(true);
            Object localObject5 = localField3.get(localClassLoader);
            i = Array.getLength(localObject5);
            Field localField4 = getFieldByName(localClass, "mDexOutputPath");
            if (localField4 == null) {
                return;
            }
            localField4.setAccessible(true);
            Object localObject6 = localField4.get(localClassLoader);
            Method localMethod = localClass.getDeclaredMethod(
                    "generateOutputName", new Class[] { String.class,
                            String.class });
            localMethod.setAccessible(true);
            Object localObject7 = localMethod.invoke(localClassLoader,
                    new Object[] { zipFile, localObject6 });
            DexFile localDexFile = DexFile.loadDex(zipFile,
                    (String) localObject7, 0);
            Object localObject8 = Array.newInstance(DexFile.class, i + 1);
            for (int m = 0; m < i; m++) {
                Array.set(localObject8, m, Array.get(localObject5, m));
            }
            Array.set(localObject8, i, localDexFile);
            localField3.set(localClassLoader, localObject8);
            Field localField5 = getFieldByName(localClass, "mLibPaths");
            if (localField5 == null) {
                return;
            }
            localField5.setAccessible(true);
            Object localObject9 = localField5.get(localClassLoader);
            i = Array.getLength(localObject9);
            Object localObject10 = Array.newInstance(String.class, i + 1);
            for (int n = 0; n < i; n++) {
                Array.set(localObject10, n, Array.get(localObject9, n));
            }
            Array.set(localObject10, i, nativeLibDir);
            localField5.set(localClassLoader, localObject10);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchMethodException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InvocationTargetException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static void addClassLoaderElementWithoutPathList(
            Context context, String zipFile, String nativeLibDir,
            String zipPath) {
        Log.d(Log.TAG, "");
        try {
            ClassLoader localClassLoader = context.getClassLoader();
            Class localClass = localClassLoader.getClass();
            Field localField1 = getFieldByName(localClass, "mFiles");
            if (localField1 == null) {
                return;
            }
            localField1.setAccessible(true);
            Object localObject1 = localField1.get(localClassLoader);
            int i = Array.getLength(localObject1);
            Object localObject2 = Array.newInstance(File.class, i + 1);
            for (int j = 0; j < i; j++) {
                Array.set(localObject2, j, Array.get(localObject1, j));
            }
            Array.set(localObject2, i, new File(zipFile));
            localField1.set(localClassLoader, localObject2);
            Field localField2 = getFieldByName(localClass, "mPaths");
            if (localField2 == null) {
                return;
            }
            localField2.setAccessible(true);
            Object localObject3 = localField2.get(localClassLoader);
            i = Array.getLength(localObject3);
            Object localObject4 = Array.newInstance(String.class, i + 1);
            for (int k = 0; k < i; k++) {
                Array.set(localObject4, k, Array.get(localObject3, k));
            }
            Array.set(localObject4, i, zipFile);
            localField2.set(localClassLoader, localObject4);
            Field localField3 = getFieldByName(localClass, "mZips");
            if (localField3 == null) {
                return;
            }
            localField3.setAccessible(true);
            Object localObject5 = localField3.get(localClassLoader);
            i = Array.getLength(localObject5);
            Object localObject6 = Array.newInstance(ZipFile.class, i + 1);
            for (int m = 0; m < i; m++) {
                Array.set(localObject6, m, Array.get(localObject5, m));
            }
            Array.set(localObject6, i, new ZipFile(zipFile));
            localField3.set(localClassLoader, localObject6);
            Field localField4 = getFieldByName(localClass, "mDexs");
            if (localField4 == null) {
                return;
            }
            localField4.setAccessible(true);
            Object localObject7 = localField4.get(localClassLoader);
            i = Array.getLength(localObject7);
            // j.d(paramString3);
            String str = getDexBaseName(zipFile) + ".dex";
            DexFile localDexFile = DexFile.loadDex(zipFile,
                    zipPath + "/" + str, 0);
            Object localObject8 = Array.newInstance(DexFile.class, i + 1);
            for (int n = 0; n < i; n++) {
                Array.set(localObject8, n, Array.get(localObject7, n));
            }
            Array.set(localObject8, i, localDexFile);
            localField4.set(localClassLoader, localObject8);
            Field localField5 = getFieldByName(localClass,
                    "libraryPathElements");
            if (localField5 == null)
                return;
            localField5.setAccessible(true);
            Object localObject9 = localField5.get(localClassLoader);
            List<?> localList = (List<?>) localObject9;
            i = localList.size();
            ArrayList<String> localArrayList = new ArrayList<String>();
            localArrayList.add(nativeLibDir);
            for (int i1 = 0; i1 < i; i1++) {
                localArrayList.add((String) localList.get(i1));
            }
            localField5.set(localClassLoader, localArrayList);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IOException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (SecurityException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    public static void setExistObject(Context context) {
        Log.d(Log.TAG, "");
        if (sDexElementsObj == null)
            return;
        if (sNativeLibDirObj == null) {
            return;
        }

        try {
            ClassLoader localClassLoader = context.getClassLoader();
            Class localClass1 = localClassLoader.getClass();
            Field localField1 = getFieldByName(localClass1, "pathList");
            if (localField1 == null)
                return;
            localField1.setAccessible(true);
            Object localObject = localField1.get(localClassLoader);
            Class localClass2 = localObject.getClass();
            Field localField2 = getFieldByName(localClass2, "dexElements");
            if (localField2 == null)
                return;
            localField2.setAccessible(true);
            localField2.set(localObject, sDexElementsObj);
            Field localField3 = getFieldByName(localClass2,
                    "nativeLibraryDirectories");
            if (localField3 == null)
                return;
            localField3.setAccessible(true);
            localField3.set(localObject, sNativeLibDirObj);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static void addDexFileAndElements(Class<?> clazz,
            Object pathListObj, String zipFile, String zipPath) {
        Log.d(Log.TAG, "");
        try {
            Method localMethod = clazz.getDeclaredMethod("loadDexFile",
                    new Class[] { File.class, File.class });
            if (localMethod == null) {
                return;
            }
            localMethod.setAccessible(true);
            Object localObject1 = localMethod.invoke(pathListObj, new Object[] {
                    new File(zipFile), new File(zipPath) });
            Object localObject2 = getPathListElementObj(zipFile, localObject1);
            if (localObject2 == null) {
                return;
            }
            Field localField = clazz.getDeclaredField("dexElements");
            localField.setAccessible(true);
            Object dexElementsObj = localField.get(pathListObj);
            if (sDexElementsObj == null) {
                sDexElementsObj = dexElementsObj;
            }
            int i = Array.getLength(dexElementsObj);
            Object localObject4 = Array.newInstance(localObject2.getClass(),
                    i + 1);
            for (int j = 0; j < i; j++) {
                Array.set(localObject4, j, Array.get(dexElementsObj, j));
            }
            Array.set(localObject4, i, localObject2);
            localField.set(pathListObj, localObject4);
        } catch (NoSuchMethodException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InvocationTargetException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static void addNativeLibDir(Class<?> paramClass,
            Object pathListObj, String nativeLibDir) {
        Log.d(Log.TAG, "");
        try {
            Field localField = paramClass
                    .getDeclaredField("nativeLibraryDirectories");
            localField.setAccessible(true);
            Object nativeLibDirObj = localField.get(pathListObj);
            if (sNativeLibDirObj == null)
                sNativeLibDirObj = nativeLibDirObj;
            int i = Array.getLength(nativeLibDirObj);
            Object localObject2 = Array.newInstance(File.class, i + 1);
            Array.set(localObject2, 0, new File(nativeLibDir));
            for (int j = 0; j < i; j++)
                Array.set(localObject2, j + 1, Array.get(nativeLibDirObj, j));
            localField.set(pathListObj, localObject2);
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
    }

    private static Object getPathListElementObj(String paramString, Object paramObject) {
        Log.d(Log.TAG, "");
        try {
            Class localClass = Class
                    .forName("dalvik.system.DexPathList$Element");
            Object localObject2;
            Object localObject3;
            Object localObject1;
            if (Build.VERSION.SDK_INT < 18) {
                try {
                    localObject2 = new ZipFile(paramString);
                    localObject3 = localClass.getConstructor(new Class[] {
                            File.class, ZipFile.class, DexFile.class });
                    ((Constructor) localObject3).setAccessible(true);
                    localObject1 = ((Constructor) localObject3)
                            .newInstance(new Object[] { new File(paramString),
                                    localObject2, (DexFile) paramObject });
                } catch (Exception e2) {
                    try {
                        localObject2 = localClass.getConstructor(new Class[] {
                                File.class, File.class, DexFile.class });
                        ((Constructor) localObject2).setAccessible(true);
                        localObject1 = ((Constructor) localObject2)
                                .newInstance(new Object[] {
                                        new File(paramString),
                                        new File(paramString),
                                        (DexFile) paramObject });
                    } catch (Exception e) {
                        localObject2 = localClass.getConstructor(new Class[] {
                                File.class, Boolean.TYPE, File.class,
                                DexFile.class });
                        ((Constructor) localObject2).setAccessible(true);
                        localObject3 = new File(paramString);
                        localObject1 = ((Constructor) localObject2)
                                .newInstance(new Object[] { localObject3,
                                        Boolean.valueOf(false), localObject3,
                                        (DexFile) paramObject });
                    }
                }
            } else {
                localObject2 = localClass.getConstructor(new Class[] {
                        File.class, Boolean.TYPE, File.class, DexFile.class });
                ((Constructor) localObject2).setAccessible(true);
                localObject3 = new File(paramString);
                localObject1 = ((Constructor) localObject2)
                        .newInstance(new Object[] { localObject3,
                                Boolean.valueOf(false), localObject3,
                                (DexFile) paramObject });
            }
            return localObject1;
        } catch (ClassNotFoundException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (NoSuchMethodException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalArgumentException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InstantiationException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (IllegalAccessException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (InvocationTargetException e) {
            Log.d(Log.TAG, "error : " + e);
        } catch (Exception e) {
            Log.d(Log.TAG, "error : " + e);
        }
        return null;
    }

    public static Field getFieldByName(Class<?> clazz, String fieldName) {
        Log.d(Log.TAG, "");
        try {
            if (clazz == null)
                return null;
            Field localField = clazz.getDeclaredField(fieldName);
            return localField;
        } catch (NoSuchFieldException e) {
            Log.d(Log.TAG, "error : " + e);
            try {
                Field localField = clazz.getField(fieldName);
                return localField;
            } catch (NoSuchFieldException error) {
                Log.d(Log.TAG, "error : " + error);
            }
        }
        return getFieldByName(clazz.getSuperclass(), fieldName);
    }

    private static String getDexBaseName(String paramString) {
        Log.d(Log.TAG, "");
        if ((paramString == null) || (paramString.trim().equals("")))
            return String.valueOf(System.currentTimeMillis());
        int i = paramString.lastIndexOf(File.separator);
        if (i == -1)
            return String.valueOf(System.currentTimeMillis());
        String str = paramString.substring(i + 1);
        int j = str.lastIndexOf(".");
        if (j == -1)
            return str;
        return str.substring(0, j);
    }
}
