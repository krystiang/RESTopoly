package restopoly.resources;

/**
 * Created by Krystian.Graczyk on 27.10.15.
 */
public class Place {
    private String name;

    public Place(String name){
    this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
