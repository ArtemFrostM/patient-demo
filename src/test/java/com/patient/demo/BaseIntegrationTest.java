package com.patient.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Common base class for every integration test.
 *
 * <p>The Spring TestContext Framework caches an application context per unique
 * configuration key (annotations, profiles, property sources, ...). By keeping that
 * configuration in a single base class we guarantee that <em>all</em> integration tests
 * share one cached context — the container and the Spring context are started once per
 * Gradle test run instead of once per test class.
 *
 * <p>Rules for subclasses:
 * <ul>
 *   <li>Do not add {@code @DirtiesContext} — it evicts the cached context.</li>
 *   <li>Do not add {@code @MockitoBean} / {@code @TestPropertySource} unless you accept a
 *       second, separate context being created for that test class.</li>
 *   <li>Clean up the data you create (see {@code @AfterEach} in the concrete tests).</li>
 * </ul>
 *
 * <p>{@code @AutoConfigureMockMvc} lives here (and not on the individual web tests) for the very
 * same reason: it is part of the context cache key, so declaring it once keeps every test — web or
 * not — on the single shared context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {
}

