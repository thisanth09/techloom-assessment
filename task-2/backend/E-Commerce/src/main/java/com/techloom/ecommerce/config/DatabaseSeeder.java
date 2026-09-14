package com.techloom.ecommerce.config;

import com.techloom.ecommerce.entity.Product;
import com.techloom.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
                Product.builder()
                        .name("Wireless Headphones")
                        .category("Audio")
                        .description("Noise-cancelling over-ear headphones with deep bass and all-day comfort.")
                        .price(9500.0)
                        .stock(12)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Smart Watch")
                        .category("Wearables")
                        .description("Track health, messages, workouts, and notifications from your wrist.")
                        .price(18500.0)
                        .stock(9)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Gaming Mouse")
                        .category("Accessories")
                        .description("RGB precision mouse built for fast reaction times and smooth control.")
                        .price(4200.0)
                        .stock(18)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1527814050087-3d134e7f0f8d?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Bluetooth Speaker")
                        .category("Audio")
                        .description("Portable speaker with crisp sound, deep bass, and waterproof design.")
                        .price(7600.0)
                        .stock(14)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1518444065439-e933c06ce9cd?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Laptop Stand")
                        .category("Office")
                        .description("Adjustable stand to improve posture and airflow during long work sessions.")
                        .price(3200.0)
                        .stock(25)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("USB-C Cable")
                        .category("Accessories")
                        .description("Reliable charging and data cable for phones, tablets, and laptops.")
                        .price(1200.0)
                        .stock(35)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("4K Smart TV")
                        .category("Electronics")
                        .description("Ultra HD display with vibrant visuals, built-in apps, and voice control.")
                        .price(48900.0)
                        .stock(7)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Fitness Tracker")
                        .category("Wearables")
                        .description("Monitor heart rate, steps, workouts, sleep, and daily movement in real time.")
                        .price(6200.0)
                        .stock(20)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1576243340888-8ea5a0d1d9b7?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Mechanical Keyboard")
                        .category("Accessories")
                        .description("Tactile switches, warm backlighting, and durable build for focused work.")
                        .price(6800.0)
                        .stock(16)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=900&q=80")
                        .build(),
                Product.builder()
                        .name("Portable SSD")
                        .category("Storage")
                        .description("Fast, lightweight storage for backups, media, and large work files.")
                        .price(9800.0)
                        .stock(11)
                        .active(true)
                        .imageUrl("https://images.unsplash.com/photo-1585771724684-38269d6639fd?auto=format&fit=crop&w=900&q=80")
                        .build()
        );

        productRepository.saveAll(products);
    }
}
