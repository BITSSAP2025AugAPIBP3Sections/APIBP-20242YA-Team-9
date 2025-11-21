# API Caching Testing Guide

## 🧪 How to Test Cache Implementation

### 1. Start the Application

```bash
cd backend
mvn spring-boot:run
```

### 2. Test Cache Hit/Miss with Job Search

#### First Request (Cache MISS - hits database)
```bash
curl --request GET \
  --url 'http://localhost:8080/api/v1/applicant/jobs?location=SF&title=Engineer' \
  --header 'authorization: Bearer <your-token>'
```
⏱️ Response time: ~500ms (database query)

#### Second Request (Cache HIT - from cache)
```bash
curl --request GET \
  --url 'http://localhost:8080/api/v1/applicant/jobs?location=SF&title=Engineer' \
  --header 'authorization: Bearer <your-token>'
```
⚡ Response time: ~10-20ms (from cache)

### 3. View Cache Statistics (Admin Only)

```bash
curl --request GET \
  --url 'http://localhost:8080/api/v1/cache/stats' \
  --header 'authorization: Bearer <admin-token>'
```

**Expected Response:**
```json
{
  "status": "success",
  "data": {
    "jobs": {
      "hitCount": 1,
      "missCount": 1,
      "hitRate": "50.00%",
      "evictionCount": 0,
      "estimatedSize": 1
    }
  }
}
```

### 4. Test Cache Invalidation

#### Create a new job (should evict cache)
```bash
curl --request POST \
  --url 'http://localhost:8080/api/v1/company/jobs' \
  --header 'authorization: Bearer <company-token>' \
  --header 'content-type: application/json' \
  --data '{
    "title": "Software Engineer",
    "description": "Great opportunity",
    "location": "SF",
    "salaryRange": "100000-150000"
  }'
```

#### Search again (should be cache MISS)
```bash
curl --request GET \
  --url 'http://localhost:8080/api/v1/applicant/jobs?location=SF&title=Engineer' \
  --header 'authorization: Bearer <your-token>'
```
⏱️ Response time: ~500ms (database query - cache was evicted)

### 5. Monitor Cache Performance

#### Get all cache names
```bash
curl --request GET \
  --url 'http://localhost:8080/api/v1/cache/names' \
  --header 'authorization: Bearer <admin-token>'
```

#### Clear specific cache
```bash
curl --request DELETE \
  --url 'http://localhost:8080/api/v1/cache/jobs' \
  --header 'authorization: Bearer <admin-token>'
```

#### Clear all caches
```bash
curl --request DELETE \
  --url 'http://localhost:8080/api/v1/cache/all' \
  --header 'authorization: Bearer <admin-token>'
```

### 6. Performance Testing with Apache Bench

```bash
# Test without cache (first time)
ab -n 100 -c 10 -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/applicant/jobs?location=SF"

# Results: ~500ms per request

# Test with cache (subsequent requests)
ab -n 100 -c 10 -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/applicant/jobs?location=SF"

# Results: ~10-20ms per request (25-50x faster!)
```

### 7. Expected Cache Behavior

| Scenario | Cache Status | Database Hit | Response Time |
|----------|-------------|--------------|---------------|
| First search | MISS | Yes | ~500ms |
| Same search within 2 min | HIT | No | ~10-20ms |
| After 2 minutes | MISS | Yes | ~500ms |
| After job update | MISS | Yes | ~500ms |
| Different search params | MISS | Yes | ~500ms |

### 8. Verify Logs

Enable cache logging in application.properties:
```properties
logging.level.org.springframework.cache=DEBUG
```

Look for log entries like:
```
Cache hit for key: 'SF_Engineer_null_null'
Cache miss for key: 'NY_Developer_null_null'
Evicting cache entries: [jobs, activeJobs]
```

## 🎯 Success Criteria

✅ Cache hit rate > 70% for repeated queries  
✅ Response time < 50ms for cached requests  
✅ Database queries reduced by 70-80%  
✅ Cache properly evicted on data changes  
✅ No stale data served to users  

## 🐛 Troubleshooting

### Cache not working?
1. Check if `@EnableCaching` is present in CacheConfig
2. Verify Caffeine dependency in pom.xml
3. Check method is public (caching doesn't work on private methods)
4. Ensure method is called from outside the class (Spring AOP requirement)

### Getting stale data?
1. Check cache eviction logic in service methods
2. Verify TTL configuration in CacheConfig
3. Clear cache manually via API endpoint

### Low hit rate?
1. Review cache key strategy (too many unique keys?)
2. Adjust TTL (too short?)
3. Increase cache size in configuration

---

**Happy Testing! 🚀**
