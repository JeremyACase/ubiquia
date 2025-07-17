package org.ubiquia.test.belief.state.generator.model;


/**
 * The model of an animal.
 */
public class Animal extends BaseModel {
    protected ColorType color;
    protected Person owner;
    protected Name name;
    protected Float height;
    protected Float weight;

    @Override
    public String getModelType() {
        return "Animal";
    }


    public ColorType getColor() {
        return color;
    }

    public void setColor(ColorType color) {
        this.color = color;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }
}
