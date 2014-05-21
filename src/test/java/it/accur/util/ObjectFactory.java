package it.accur.util;

public abstract class ObjectFactory {
    private static class ObjectFactoryFinder 
    extends AbstractFactoryFinder <ObjectFactory> {
        ObjectFactoryFinder () {
            super(ObjectFactory.class, "it.accur.util.ObjectFactory",
                  "objectfactory.properties", null);
        }
        public ObjectFactory getFactory () {
            return find();
        }
    }

    private static ObjectFactoryFinder finder = new ObjectFactoryFinder();

    abstract Object getObject ();

    public static ObjectFactory getFactory () {
        return finder.find();
    }
}
