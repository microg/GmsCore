package com.google.android.gms.dynamic;

import android.os.IBinder;
import android.view.View;

import java.lang.reflect.Field;

public class ObjectWrapper<T> extends IObjectWrapper.Stub {
	private final T t;

	public ObjectWrapper(T t) {
		this.t = t;
	}

	public static Object unwrap(IObjectWrapper obj) {
		if (obj instanceof ObjectWrapper) {
			return ((ObjectWrapper) obj).t;
		}
		IBinder binder = obj.asBinder();
		Field[] fields = binder.getClass().getDeclaredFields();
		if (fields.length != 1) {
			throw new IllegalArgumentException("The concrete class implementing IObjectWrapper must have exactly *one* declared private field for the wrapped object.  Preferably, this is an instance of the ObjectWrapper<T> class.");
		}
		Field field = fields[0];
		if (!field.isAccessible()) {
			field.setAccessible(true);
			try {
				Object wrapped = field.get(binder);
				return wrapped;
			} catch (NullPointerException localNullPointerException) {
				throw new IllegalArgumentException("Binder object is null.", localNullPointerException);
			} catch (IllegalArgumentException localIllegalArgumentException) {
				throw new IllegalArgumentException("remoteBinder is the wrong class.", localIllegalArgumentException);
			} catch (IllegalAccessException localIllegalAccessException) {
				throw new IllegalArgumentException("Could not access the field in remoteBinder.", localIllegalAccessException);
			}
		} else {
			throw new IllegalArgumentException("The concrete class implementing IObjectWrapper must have exactly one declared *private* field for the wrapped object. Preferably, this is an instance of the ObjectWrapper<T> class.");
		}
	}

	public static <T> ObjectWrapper<T> wrap(T t) {
		return new ObjectWrapper<T>(t);
	}
}