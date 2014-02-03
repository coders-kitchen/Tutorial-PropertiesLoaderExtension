package com.coderskitchen.propertyloader;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PropertyLoaderExtension implements Extension {

	final Map<Field, Object> fieldValues = new HashMap<Field, Object>();

	public <T> void initializePropertyLoading(final @Observes ProcessInjectionTarget<T> pit) {
		AnnotatedType<T> at = pit.getAnnotatedType();
		if(!at.isAnnotationPresent(PropertyyFile.class)) {
			return;
		}
		PropertyyFile propertyyFile = at.getAnnotation(PropertyyFile.class);
		String filename = propertyyFile.value();
		InputStream propertiesStream = getClass().getResourceAsStream("/" + filename);
		Properties properties = new Properties();
		try {
			properties.load(propertiesStream);
			assignPropertiesToFields(at.getFields(), properties);

		} catch (IOException e) {
			e.printStackTrace();
		}

		final InjectionTarget<T> it = pit.getInjectionTarget();
		InjectionTarget<T> wrapped = new InjectionTarget<T>() {
			@Override
			public void inject(T instance, CreationalContext<T> ctx) {
				it.inject(instance, ctx);
				for (Map.Entry<Field, Object> property: fieldValues.entrySet()) {
					try {
						Field key = property.getKey();
						key.setAccessible(true);
						Class<?> baseType = key.getType();
						String value = property.getValue().toString();
						if (baseType == String.class) {
							key.set(instance, value);
						}  else if (baseType == Integer.class) {
							key.set(instance, Integer.valueOf(value));
						} else {
							String message = "Type " + baseType + " of Field " + key.getName() + " not recognized yet!";
							System.out.println(message);
							pit.addDefinitionError(new InjectionException(message));
						}
					} catch (Exception e) {
						pit.addDefinitionError(new InjectionException(e));
					}
				}
			}


			@Override
			public void postConstruct(T instance) {
				it.postConstruct(instance);
			}


			@Override
			public void preDestroy(T instance) {
				it.dispose(instance);
			}


			@Override
			public void dispose(T instance) {
				it.dispose(instance);
			}


			@Override
			public Set<InjectionPoint> getInjectionPoints() {
				return it.getInjectionPoints();
			}


			@Override
			public T produce(CreationalContext<T> ctx) {
				return it.produce(ctx);
			}
		};
		pit.setInjectionTarget(wrapped);
	}

	private <T> void assignPropertiesToFields(Set<AnnotatedField<? super T>> fields, Properties properties) {
		for (AnnotatedField<? super T> field : fields) {
			if(field.isAnnotationPresent(Propertyy.class)) {
				Propertyy propertyy = field.getAnnotation(Propertyy.class);
				Object value = properties.get(propertyy.value());
				Field memberField = field.getJavaMember();
				fieldValues.put(memberField, value);
			}
		}
	}
}
