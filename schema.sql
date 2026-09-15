-- ========================================================
-- Vehicle Rental Management System - Database Schema
-- Database: vehicle_rental_db
-- ========================================================

CREATE DATABASE IF NOT EXISTS vehicle_rental_db;
USE vehicle_rental_db;

-- 1. Users Table (System Administrators & Staff)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('Admin', 'Staff') NOT NULL DEFAULT 'Staff',
    status ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active'
);

-- Seed default Administrator account
INSERT INTO users (username, password, full_name, role, status)
VALUES ('admin', 'admin123', 'System Administrator', 'Admin', 'Active')
ON DUPLICATE KEY UPDATE username=username;

-- 2. Vehicle Categories Table
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL,
    description TEXT
);

-- Seed default categories
INSERT INTO categories (category_name, description) VALUES
('Economy', 'Fuel-efficient compact cars for daily city commuting'),
('Sedan', 'Comfortable 5-seater family sedans with spacious boot'),
('SUV', 'Powerful sports utility vehicles suitable for all terrains'),
('Luxury', 'Premium executive luxury vehicles with chauffeur-grade comfort')
ON DUPLICATE KEY UPDATE category_name=category_name;

-- 3. Vehicles Fleet Table
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    reg_number VARCHAR(20) UNIQUE NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    category_id INT,
    daily_rate DECIMAL(10,2) NOT NULL,
    status ENUM('Available', 'Rented', 'Under Maintenance') DEFAULT 'Available',
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL
);

-- 4. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    license_number VARCHAR(50) NOT NULL,
    address TEXT
);

-- 5. Rentals & Bookings Table
CREATE TABLE IF NOT EXISTS rentals (
    rental_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    pickup_date DATE NOT NULL,
    return_date DATE NOT NULL,
    actual_return_date DATE NULL,
    rental_duration_days INT NOT NULL,
    daily_rate DECIMAL(10,2) NOT NULL,
    total_rent DECIMAL(10,2) NOT NULL,
    security_deposit DECIMAL(10,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    late_fee DECIMAL(10,2) DEFAULT 0,
    damage_fee DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    status ENUM('Active', 'Returned', 'Cancelled') DEFAULT 'Active',
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- 6. Maintenance & Servicing Table
CREATE TABLE IF NOT EXISTS maintenance (
    maintenance_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    cost DECIMAL(10,2) NOT NULL,
    maintenance_date DATE NOT NULL,
    description TEXT,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- 7. System Settings Table
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);

-- Default system settings
INSERT INTO system_settings (setting_key, setting_value) VALUES 
('COMPANY_NAME', 'Apex Drive Vehicle Rentals'),
('TAX_PERCENTAGE', '18.0'),
('LATE_FEE_PER_DAY', '500.0')
ON DUPLICATE KEY UPDATE setting_key=setting_key;
