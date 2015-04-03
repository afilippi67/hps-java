package org.hps.conditions.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation for providing converter configuration for {@link org.hps.conditions.api.ConditionsObject}
 * classes.
 *
 * @see AbstractConditionsObjectConverter
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Converter {
    /**
     * Get the action to perform in the converter when multiple conditions are found for the current configuration of
     * run number, detector and tag in the manager.
     *
     * @return The multiple collections action.
     */
    MultipleCollectionsAction multipleCollectionsAction() default MultipleCollectionsAction.ERROR;

    /**
     * Get a custom converter class for the type. (Optional)
     *
     * @return The custom converter for the type.
     */
    Class<?> converter() default AbstractConditionsObjectConverter.class;
}
