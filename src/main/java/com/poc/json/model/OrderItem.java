package com.poc.json.model;

public class OrderItem {

    private String productId;
    private String name;
    private int quantity;
    private long unitPrice;

    public OrderItem() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }

    public long getTotalPrice() { return (long) quantity * unitPrice; }

    @Override
    public String toString() {
        return String.format("OrderItem{name='%s', qty=%d, unitPrice=%,d, total=%,d}",
                name, quantity, unitPrice, getTotalPrice());
    }
}
